package fu.libmanagement.engagement.controller;

import fu.libmanagement.auth.dto.SessionUser;
import fu.libmanagement.catalog.dto.BookSummary;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.engagement.service.FavoriteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

//UC9: Danh sách yêu thích
@Controller
@RequestMapping("/reader/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       HttpServletRequest request, Model model) {
        SessionUser user = SessionUser.get(request);
        Page<BookSummary> result = favoriteService.list(user.getId(), page);

        model.addAttribute("result", result);
        model.addAttribute("books", result.getContent());
        return "engagement/favorite-list";
    }

    @PostMapping("/{bookId}/add")
    public String add(@PathVariable Long bookId, HttpServletRequest request,
                      RedirectAttributes redirect) {
        SessionUser user = SessionUser.get(request);
        try {
            favoriteService.add(user.getId(), bookId);
            redirect.addFlashAttribute("message", "Đã thêm vào danh sách yêu thích");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/books/" + bookId;
    }

    @PostMapping("/{bookId}/remove")
    public String remove(@PathVariable Long bookId,
                         @RequestParam(defaultValue = "list") String from,
                         HttpServletRequest request, RedirectAttributes redirect) {
        SessionUser user = SessionUser.get(request);
        favoriteService.remove(user.getId(), bookId);
        redirect.addFlashAttribute("message", "Đã bỏ khỏi danh sách yêu thích");

        if ("detail".equals(from)) {
            return "redirect:/books/" + bookId;
        }
        return "redirect:/reader/favorites";
    }
}
