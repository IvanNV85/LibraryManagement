package fu.libmanagement.inventory.controller;

import fu.libmanagement.auth.dto.SessionUser;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.common.enums.Role;
import fu.libmanagement.inventory.service.PurchaseOrderService;
import fu.libmanagement.inventory.service.PurchaseOrderService.Line;
import fu.libmanagement.inventory.service.PurchaseOrderService.Receipt;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Luồng phiếu nhập: thủ thư lập phiếu và nhận kho, Admin duyệt; dùng chung trang danh sách và chi tiết. */
@Controller
@RequiredArgsConstructor
public class PurchaseOrderController {
    private final PurchaseOrderService service;

    /** Hiển thị danh sách phiếu nhập; chuyển Admin đang dùng URL thủ thư về URL quản trị tương ứng. */
    @GetMapping({"/librarian/purchase-orders", "/admin/purchase-orders"})
    public String list(Model model, HttpServletRequest request) {
        if (SessionUser.get(request).isAdmin() && request.getRequestURI().substring(request.getContextPath().length()).startsWith("/librarian/")) {
            return "redirect:/admin/purchase-orders";
        }
        model.addAttribute("orders", service.list());
        return "inventory/order-list";
    }

    /** Chỉ thủ thư được mở form lập phiếu; nạp nhà cung cấp để hiển thị danh sách lựa chọn. */
    @GetMapping("/librarian/purchase-orders/new")
    public String createForm(HttpServletRequest request, Model model) {
        requireLibrarian(request);
        model.addAttribute("suppliers", service.suppliers());
        return "inventory/order-form";
    }

    /**
     * Nhận các dòng sách từ form, chuyển số lượng/đơn giá sang kiểu số và tạo phiếu chờ Admin duyệt.
     * Thành công chuyển đến chi tiết; lỗi dữ liệu chuyển lại trang tạo kèm thông báo flash.
     */
    @PostMapping("/librarian/purchase-orders")
    public String create(@RequestParam long supplierId, @RequestParam(defaultValue="") String note,
                         HttpServletRequest request, RedirectAttributes redirect) {
        SessionUser user = requireLibrarian(request);
        try {
            List<String> isbn = values(request, "isbn");
            List<String> title = values(request, "title");
            List<String> quantity = values(request, "quantity");
            List<String> unitPrice = values(request, "unitPrice");
            // Các input trùng tên được gửi thành danh sách; cùng chỉ số phải thuộc cùng một dòng sách.
            if (isbn.size() != title.size() || isbn.size() != quantity.size() || isbn.size() != unitPrice.size())
                throw new BusinessException("Dữ liệu dòng sách không đầy đủ");
            List<Line> lines = new ArrayList<>();
            for (int i=0; i<isbn.size(); i++) lines.add(new Line(isbn.get(i), title.get(i), Integer.valueOf(quantity.get(i)), new BigDecimal(unitPrice.get(i))));
            long id = service.create(supplierId, note, lines, user.getId());
            redirect.addFlashAttribute("message", "Đã tạo phiếu chờ duyệt");
            return "redirect:/librarian/purchase-orders/" + id;
        } catch (BusinessException | NumberFormatException e) {
            redirect.addFlashAttribute("error", e instanceof NumberFormatException ? "Số lượng hoặc đơn giá không hợp lệ" : e.getMessage());
            return "redirect:/librarian/purchase-orders/new";
        }
    }

    /** Nạp thông tin phiếu và các dòng sách cho trang chi tiết dùng chung của Admin và thủ thư. */
    @GetMapping({"/librarian/purchase-orders/{id}", "/admin/purchase-orders/{id}"})
    public String detail(@PathVariable long id, Model model, HttpServletRequest request) {
        if (SessionUser.get(request).isAdmin() && request.getRequestURI().substring(request.getContextPath().length()).startsWith("/librarian/")) {
            return "redirect:/admin/purchase-orders/" + id;
        }
        model.addAttribute("order", service.order(id));
        model.addAttribute("items", service.items(id));
        return "inventory/order-detail";
    }

    /** Admin duyệt phiếu, ghi nhận người duyệt từ session; quyền Admin được kiểm tra theo tiền tố URL. */
    @PostMapping("/admin/purchase-orders/{id}/approve")
    public String approve(@PathVariable long id, HttpServletRequest request, RedirectAttributes redirect) {
        try {
            service.approve(id, SessionUser.get(request).getId());
            redirect.addFlashAttribute("message", "Đã duyệt phiếu nhập");
        } catch (BusinessException e) { redirect.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/purchase-orders/" + id;
    }

    /**
     * Chỉ thủ thư được gửi số lượng thực nhận và ghi chú theo từng dòng để service nhận kho, tạo bản sao.
     * Chuyển về trang chi tiết cùng thông báo thành công hoặc lỗi nghiệp vụ/định dạng số.
     */
    @PostMapping("/librarian/purchase-orders/{id}/receive")
    public String receive(@PathVariable long id, HttpServletRequest request, RedirectAttributes redirect) {
        requireLibrarian(request);
        try {
            List<String> itemId = values(request, "itemId");
            List<String> receivedQuantity = values(request, "receivedQuantity");
            List<String> discrepancyNote = values(request, "discrepancyNote");
            // Ghép ID dòng phiếu với số lượng thực nhận và ghi chú tương ứng trước khi gọi service.
            if (itemId.size() != receivedQuantity.size() || itemId.size() != discrepancyNote.size())
                throw new BusinessException("Dữ liệu nhận hàng không đầy đủ");
            List<Receipt> rows = new ArrayList<>();
            for (int i=0; i<itemId.size(); i++) rows.add(new Receipt(Long.parseLong(itemId.get(i)), Integer.valueOf(receivedQuantity.get(i)), discrepancyNote.get(i)));
            service.receive(id, rows);
            redirect.addFlashAttribute("message", "Đã hoàn tất nhận kho và tạo bản sao");
        } catch (BusinessException | NumberFormatException e) {
            redirect.addFlashAttribute("error", e instanceof NumberFormatException ? "Số lượng thực nhận không hợp lệ" : e.getMessage());
        }
        return "redirect:/librarian/purchase-orders/" + id;
    }

    /** Đọc nhóm input cùng tên của form nhiều dòng; báo lỗi nếu request thiếu nhóm dữ liệu đó. */
    private List<String> values(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null) throw new BusinessException("Dữ liệu dòng sách không đầy đủ");
        return Arrays.asList(values);
    }

    /**
     * Giới hạn thao tác lập phiếu/nhận kho cho đúng vai trò LIBRARIAN.
     * Cần kiểm tra thêm vì RoleInterceptor cho phép cả Admin truy cập nhóm URL /librarian/**.
     */
    private SessionUser requireLibrarian(HttpServletRequest request) {
        SessionUser user = SessionUser.get(request);
        if (user == null || user.getRole() != Role.LIBRARIAN) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return user;
    }
}
