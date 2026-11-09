package fu.libmanagement.borrowing.controller;

import fu.libmanagement.auth.dto.SessionUser;
import fu.libmanagement.borrowing.dto.ReturnBookForm;
import fu.libmanagement.borrowing.dto.ReturnPreviewDto;
import fu.libmanagement.borrowing.entity.Loan;
import fu.libmanagement.borrowing.service.LibrarianLoanService;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.inventory.entity.BookCopy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * Controller tiếp nhận và ghi nhận trả sách tại quầy dành cho Thủ thư (UC18).
 * Tiền tố /librarian/** được phân quyền tự động cho LIBRARIAN & ADMIN bởi RoleInterceptor.
 */
@Slf4j
@Controller
@RequestMapping("/librarian/loans/return")
@RequiredArgsConstructor
public class LibrarianReturnController {

    private final LibrarianLoanService librarianLoanService;

    /**
     * Màn hình Ghi nhận trả sách (Form quét barcode/mã phiếu + Danh sách sách đang lưu hành).
     */
    @GetMapping
    public String returnScreen(@RequestParam(defaultValue = "") String keyword,
                               @RequestParam(defaultValue = "ALL") String status,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
        Page<Loan> result = librarianLoanService.getReturningLoans(keyword, status, page);
        Map<Long, BookCopy> copyMap = librarianLoanService.getReturnCopyMap(result);

        model.addAttribute("result", result);
        model.addAttribute("loans", result.getContent());
        model.addAttribute("copyMap", copyMap);
        model.addAttribute("keyword", keyword.trim());
        model.addAttribute("status", status);

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ReturnBookForm());
        }

        return "librarian/borrowing/return";
    }

    /**
     * Xử lý hoàn tất ghi nhận trả sách (cập nhật trạng thái phiếu, trả kho, tính phạt trễ hạn).
     */
    @PostMapping
    public String processReturn(@ModelAttribute ReturnBookForm form,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        SessionUser user = SessionUser.get(request);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            String successMessage = librarianLoanService.returnBook(form, user.getId());
            redirectAttributes.addFlashAttribute("message", successMessage);
        } catch (BusinessException e) {
            log.warn("Lỗi khi ghi nhận trả sách: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi hệ thống khi ghi nhận trả sách", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra trong quá trình xử lý: " + e.getMessage());
        }

        return "redirect:/librarian/loans/return";
    }

    /**
     * API JSON tra cứu thông tin phiếu mượn qua Barcode hoặc Mã phiếu mượn (#ID).
     */
    @GetMapping({"/lookup", "/librarian/loans/api/return-lookup"})
    @ResponseBody
    public ResponseEntity<?> returnLookup(@RequestParam String identifier) {
        try {
            ReturnPreviewDto dto = librarianLoanService.getReturnPreview(identifier);
            return ResponseEntity.ok(dto);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

