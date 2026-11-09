package fu.libmanagement.borrowing.controller;

import fu.libmanagement.auth.dto.SessionUser;
import fu.libmanagement.borrowing.dto.BorrowSummaryDto;
import fu.libmanagement.borrowing.entity.Loan;
import fu.libmanagement.borrowing.service.LibrarianLoanService;
import fu.libmanagement.common.enums.LoanStatus;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.inventory.entity.BookCopy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller xử lý màn hình Duyệt yêu cầu mượn online dành cho Thủ thư (UC16).
 * Tiền tố /librarian/** được bảo vệ bởi RoleInterceptor.
 */
@Controller
@RequestMapping("/librarian/borrow-requests")
@RequiredArgsConstructor
public class LibrarianBorrowRequestController {

    private final LibrarianLoanService librarianLoanService;

    /**
     * Màn hình danh sách yêu cầu mượn online.
     * Mặc định lọc các yêu cầu PENDING (chờ duyệt).
     */
    @GetMapping
    public String listRequests(@RequestParam(defaultValue = "") String keyword,
                               @RequestParam(required = false) String status,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
        LoanStatus queryStatus = null;
        String selectedStatusStr = "ALL";

        if (status == null) {
            // Lần đầu vào trang (không có param): mặc định hiển thị PENDING
            queryStatus = LoanStatus.PENDING;
            selectedStatusStr = "PENDING";
        } else if ("ALL".equalsIgnoreCase(status.trim()) || status.trim().isEmpty()) {
            // Người dùng chủ động chọn "Tất cả trạng thái"
            queryStatus = null;
            selectedStatusStr = "ALL";
        } else {
            try {
                queryStatus = LoanStatus.valueOf(status.trim().toUpperCase());
                selectedStatusStr = queryStatus.name();
            } catch (IllegalArgumentException e) {
                queryStatus = null;
                selectedStatusStr = "ALL";
            }
        }

        Page<Loan> result = librarianLoanService.getBorrowRequests(keyword, queryStatus, page);
        BorrowSummaryDto summary = librarianLoanService.getSummary();

        model.addAttribute("result", result);
        model.addAttribute("requests", result.getContent());
        model.addAttribute("summary", summary);
        model.addAttribute("keyword", keyword.trim());
        model.addAttribute("selectedStatus", selectedStatusStr);
        model.addAttribute("statuses", List.of(
                LoanStatus.PENDING,
                LoanStatus.APPROVED,
                LoanStatus.BORROWED,
                LoanStatus.REJECTED,
                LoanStatus.EXPIRED,
                LoanStatus.CANCELLED
        ));

        return "librarian/borrowing/requests";
    }

    /**
     * API JSON lấy danh sách các bản sao còn sẵn sàng của một cuốn sách (dùng cho Modal chọn bản sao khi duyệt).
     */
    @GetMapping("/api/copies/{bookId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAvailableCopies(@PathVariable Long bookId) {
        List<BookCopy> copies = librarianLoanService.getAvailableCopies(bookId);
        List<Map<String, Object>> response = copies.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("barcode", c.getBarcode());
            map.put("shelfLocation", c.getShelfLocation() != null ? c.getShelfLocation() : "Chưa xếp giá");
            return map;
        }).toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Duyệt yêu cầu mượn: gán bản sao vật lý và đặt hạn đến nhận sách.
     */
    @PostMapping("/{id}/approve")
    public String approveRequest(@PathVariable Long id,
                                 @RequestParam Long copyId,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        SessionUser user = SessionUser.get(request);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            librarianLoanService.approveRequest(id, copyId, user.getId());
            redirectAttributes.addFlashAttribute("message", "Đã duyệt yêu cầu mượn thành công! Bản sao đã được giữ tại quầy trong 3 ngày.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/librarian/borrow-requests?status=APPROVED";
    }

    /**
     * Từ chối yêu cầu mượn sách kèm lý do.
     */
    @PostMapping("/{id}/reject")
    public String rejectRequest(@PathVariable Long id,
                                @RequestParam(required = false) String reason,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        SessionUser user = SessionUser.get(request);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            librarianLoanService.rejectRequest(id, reason, user.getId());
            redirectAttributes.addFlashAttribute("message", "Đã từ chối yêu cầu mượn.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/librarian/borrow-requests?status=REJECTED";
    }

    /**
     * Giao sách cho độc giả khi độc giả đến nhận trực tiếp tại quầy (APPROVED -> BORROWED).
     */
    @PostMapping("/{id}/issue")
    public String issueLoan(@PathVariable Long id,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        SessionUser user = SessionUser.get(request);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            librarianLoanService.issueApprovedLoan(id, user.getId());
            redirectAttributes.addFlashAttribute("message", "Đã xác nhận giao sách cho độc giả thành công! Phiếu mượn đã có hiệu lực.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/librarian/borrow-requests?status=BORROWED";
    }
}

