package fu.libmanagement.engagement.controller;

import fu.libmanagement.auth.dto.SessionUser;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.engagement.dto.ReviewRequest;
import fu.libmanagement.engagement.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

//UC10: Gửi đánh giá, chỉ độc giả
@Controller
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/reader/books/{bookId}/review")
    public String save(@PathVariable Long bookId,
                       @ModelAttribute ReviewRequest form,
                       HttpServletRequest request,
                       RedirectAttributes redirect) {
        SessionUser user = SessionUser.get(request);
        try {
            reviewService.save(user.getId(), bookId, form);
            redirect.addFlashAttribute("message", "Đã lưu đánh giá của bạn");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/books/" + bookId;
    }
}
