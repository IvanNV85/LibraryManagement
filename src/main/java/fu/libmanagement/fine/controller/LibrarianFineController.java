package fu.libmanagement.fine.controller;

import fu.libmanagement.auth.dto.SessionUser;
import fu.libmanagement.common.enums.FineStatus;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.fine.dto.FineSummaryDto;
import fu.libmanagement.fine.entity.Fine;
import fu.libmanagement.fine.service.FineService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller xử lý màn hình quản lý và thu tiền phạt dành cho Thủ thư (UC19).
 * Tiền tố /librarian/** được bảo vệ bởi RoleInterceptor.
 */
@Controller
@RequestMapping("/librarian/fines")
@RequiredArgsConstructor
public class LibrarianFineController {

    private final FineService fineService;

    /**
     * Danh sách các khoản phạt (hỗ trợ tìm kiếm, lọc theo trạng thái và phân trang).
     */
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(required = false) FineStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<Fine> result = fineService.getFines(keyword, status, page);
        FineSummaryDto summary = fineService.getSummary();

        model.addAttribute("result", result);
        model.addAttribute("fines", result.getContent());
        model.addAttribute("summary", summary);
        model.addAttribute("keyword", keyword.trim());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", FineStatus.values());

        return "librarian/fines/list";
    }

    /**
     * Xử lý xác nhận thu tiền phạt trực tiếp tại quầy từ độc giả.
     */
    @PostMapping("/{id}/pay")
    public String payFine(@PathVariable Long id,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        SessionUser user = SessionUser.get(request);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            fineService.payFineAtCounter(id, user.getId());
            redirectAttributes.addFlashAttribute("message", "Đã thu tiền phạt thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/librarian/fines";
    }
}

