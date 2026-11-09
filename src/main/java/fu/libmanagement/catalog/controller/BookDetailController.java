package fu.libmanagement.catalog.controller;

import fu.libmanagement.auth.dto.SessionUser;
import fu.libmanagement.catalog.service.BookDetailService;
import fu.libmanagement.engagement.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

//UC8: Xem chi tiết sách(Guest + Reader)
@Controller
@RequiredArgsConstructor
public class BookDetailController {

    private final BookDetailService bookDetailService;
    private final ReviewService reviewService;

    @GetMapping("/books/{id}")
    public String detail(@PathVariable Long id, HttpServletRequest request, Model model) {
        SessionUser user = SessionUser.get(request);
        Long readerId = (user != null && user.isReader()) ? user.getId() : null;

        model.addAttribute("book", bookDetailService.getDetail(id, readerId));
        model.addAttribute("reviews", bookDetailService.getLatestReviews(id));

        //Form đánh giá của độc giả
        if (readerId != null) {
            model.addAttribute("canReview", reviewService.canReview(readerId, id));
            model.addAttribute("reviewForm", reviewService.getMyReview(readerId, id));
        }
        return "catalog/book-detail";
    }
}