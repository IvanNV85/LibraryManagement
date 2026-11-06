package fu.libmanagement.user.controller;

import fu.libmanagement.common.enums.CardStatus;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.user.dto.ReaderResponse;
import fu.libmanagement.user.service.ReaderManagementService;
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

/** UC6 Quản lý độc giả. URL dưới /librarian nên thủ thư và admin đều vào được. */
@Controller
@RequestMapping("/librarian/readers")
@RequiredArgsConstructor
public class ReaderManagementController {

    private final ReaderManagementService service;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "ALL") String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<ReaderResponse> result = service.search(keyword, status, page);

        model.addAttribute("result", result);
        model.addAttribute("readers", result.getContent());
        model.addAttribute("keyword", keyword.trim());
        model.addAttribute("status", status);
        model.addAttribute("statuses", CardStatus.values());
        return "librarian/reader-list";
    }

    @PostMapping("/{cardId}/approve")
    public String approve(@PathVariable Long cardId,
                          @RequestParam(defaultValue = "") String keyword,
                          @RequestParam(defaultValue = "ALL") String status,
                          @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes redirect) {
        return handle(() -> service.approve(cardId), "Đã duyệt thẻ", keyword, status, page, redirect);
    }

    @PostMapping("/{cardId}/lock")
    public String lock(@PathVariable Long cardId,
                       @RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "ALL") String status,
                       @RequestParam(defaultValue = "0") int page,
                       RedirectAttributes redirect) {
        return handle(() -> service.lock(cardId), "Đã khóa thẻ", keyword, status, page, redirect);
    }

    @PostMapping("/{cardId}/unlock")
    public String unlock(@PathVariable Long cardId,
                         @RequestParam(defaultValue = "") String keyword,
                         @RequestParam(defaultValue = "ALL") String status,
                         @RequestParam(defaultValue = "0") int page,
                         RedirectAttributes redirect) {
        return handle(() -> service.unlock(cardId), "Đã mở khóa thẻ", keyword, status, page, redirect);
    }

    @PostMapping("/{cardId}/extend")
    public String extend(@PathVariable Long cardId,
                         @RequestParam(defaultValue = "") String keyword,
                         @RequestParam(defaultValue = "ALL") String status,
                         @RequestParam(defaultValue = "0") int page,
                         RedirectAttributes redirect) {
        return handle(() -> service.extend(cardId), "Đã gia hạn thẻ thêm 12 tháng", keyword, status, page, redirect);
    }

    /** Chạy thao tác, báo kết quả bằng flash message, rồi quay lại đúng trang đang xem. */
    private String handle(Runnable action, String successMessage, String keyword, String status,
                          int page, RedirectAttributes redirect) {
        try {
            action.run();
            redirect.addFlashAttribute("message", successMessage);
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        redirect.addAttribute("keyword", keyword);   // addAttribute: thành tham số trên URL
        redirect.addAttribute("status", status);
        redirect.addAttribute("page", page);
        return "redirect:/librarian/readers";
    }
}
