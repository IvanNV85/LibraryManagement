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

@Controller
@RequiredArgsConstructor
public class PurchaseOrderController {
    private final PurchaseOrderService service;

    @GetMapping({"/librarian/purchase-orders", "/admin/purchase-orders"})
    public String list(Model model, HttpServletRequest request) {
        if (SessionUser.get(request).isAdmin() && request.getRequestURI().substring(request.getContextPath().length()).startsWith("/librarian/")) {
            return "redirect:/admin/purchase-orders";
        }
        model.addAttribute("orders", service.list());
        return "inventory/order-list";
    }

    @GetMapping("/librarian/purchase-orders/new")
    public String createForm(HttpServletRequest request, Model model) {
        requireLibrarian(request);
        model.addAttribute("suppliers", service.suppliers());
        return "inventory/order-form";
    }

    @PostMapping("/librarian/purchase-orders")
    public String create(@RequestParam long supplierId, @RequestParam(defaultValue="") String note,
                         HttpServletRequest request, RedirectAttributes redirect) {
        SessionUser user = requireLibrarian(request);
        try {
            List<String> isbn = values(request, "isbn");
            List<String> title = values(request, "title");
            List<String> quantity = values(request, "quantity");
            List<String> unitPrice = values(request, "unitPrice");
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

    @GetMapping({"/librarian/purchase-orders/{id}", "/admin/purchase-orders/{id}"})
    public String detail(@PathVariable long id, Model model, HttpServletRequest request) {
        if (SessionUser.get(request).isAdmin() && request.getRequestURI().substring(request.getContextPath().length()).startsWith("/librarian/")) {
            return "redirect:/admin/purchase-orders/" + id;
        }
        model.addAttribute("order", service.order(id));
        model.addAttribute("items", service.items(id));
        return "inventory/order-detail";
    }

    @PostMapping("/admin/purchase-orders/{id}/approve")
    public String approve(@PathVariable long id, HttpServletRequest request, RedirectAttributes redirect) {
        try {
            service.approve(id, SessionUser.get(request).getId());
            redirect.addFlashAttribute("message", "Đã duyệt phiếu nhập");
        } catch (BusinessException e) { redirect.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/purchase-orders/" + id;
    }

    @PostMapping("/librarian/purchase-orders/{id}/receive")
    public String receive(@PathVariable long id, HttpServletRequest request, RedirectAttributes redirect) {
        requireLibrarian(request);
        try {
            List<String> itemId = values(request, "itemId");
            List<String> receivedQuantity = values(request, "receivedQuantity");
            List<String> discrepancyNote = values(request, "discrepancyNote");
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

    private List<String> values(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null) throw new BusinessException("Dữ liệu dòng sách không đầy đủ");
        return Arrays.asList(values);
    }

    private SessionUser requireLibrarian(HttpServletRequest request) {
        SessionUser user = SessionUser.get(request);
        if (user == null || user.getRole() != Role.LIBRARIAN) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return user;
    }
}
