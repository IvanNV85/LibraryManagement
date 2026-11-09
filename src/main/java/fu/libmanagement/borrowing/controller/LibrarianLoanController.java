package fu.libmanagement.borrowing.controller;

import fu.libmanagement.auth.dto.SessionUser;
import fu.libmanagement.borrowing.dto.CopyLookupDto;
import fu.libmanagement.borrowing.dto.CounterLoanForm;
import fu.libmanagement.borrowing.dto.ReaderLookupDto;
import fu.libmanagement.borrowing.entity.Loan;
import fu.libmanagement.borrowing.service.LibrarianLoanService;
import fu.libmanagement.common.enums.LoanSource;
import fu.libmanagement.common.enums.LoanStatus;
import fu.libmanagement.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
 * Controller xử lý màn hình Lập phiếu mượn tại quầy (UC17) và Quản lý phiếu mượn thư viện.
 * Tiền tố /librarian/** được bảo vệ bởi RoleInterceptor.
 */
@Controller
@RequestMapping("/librarian/loans")
@RequiredArgsConstructor
public class LibrarianLoanController {

    private final LibrarianLoanService librarianLoanService;

    /**
     * Màn hình lập phiếu mượn trực tiếp tại quầy (UC17).
     */
    @GetMapping("/create")
    public String createForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new CounterLoanForm());
        }
        return "librarian/borrowing/counter-loan";
    }

    /**
     * Xử lý lập phiếu mượn trực tiếp tại quầy (UC17).
     */
    @PostMapping("/create")
    public String createLoan(@ModelAttribute CounterLoanForm form,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        SessionUser user = SessionUser.get(request);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Loan loan = librarianLoanService.createCounterLoan(form, user.getId());
            redirectAttributes.addFlashAttribute("message", "Lập phiếu mượn thành công! Mã phiếu: #" + loan.getId()
                    + " (Hạn trả: " + loan.getDueDate() + ").");
            return "redirect:/librarian/loans";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("form", form);
            return "librarian/borrowing/counter-loan";
        }
    }

    /**
     * API tra cứu nhanh thông tin & điều kiện độc giả tại quầy.
     */
    @GetMapping("/api/reader-lookup")
    @ResponseBody
    public ResponseEntity<?> lookupReader(@RequestParam String identifier) {
        try {
            ReaderLookupDto dto = librarianLoanService.lookupReader(identifier);
            return ResponseEntity.ok(dto);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * API tra cứu nhanh bản sao sách qua mã vạch barcode tại quầy.
     */
    @GetMapping("/api/copy-lookup")
    @ResponseBody
    public ResponseEntity<?> lookupCopy(@RequestParam String barcode) {
        try {
            CopyLookupDto dto = librarianLoanService.lookupCopy(barcode);
            return ResponseEntity.ok(dto);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Danh sách / Quản lý toàn bộ phiếu mượn thư viện (cả Online và Tại quầy).
     */
    @GetMapping
    public String listLoans(@RequestParam(defaultValue = "") String keyword,
                            @RequestParam(required = false) LoanSource source,
                            @RequestParam(required = false) LoanStatus status,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        Page<Loan> result = librarianLoanService.getAllLoans(keyword, source, status, page);

        model.addAttribute("result", result);
        model.addAttribute("loans", result.getContent());
        model.addAttribute("keyword", keyword.trim());
        model.addAttribute("selectedSource", source);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("sources", LoanSource.values());
        model.addAttribute("statuses", LoanStatus.values());

        return "librarian/borrowing/list";
    }
}

