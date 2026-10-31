package fu.libmanagement.catalog.controller;

import fu.libmanagement.catalog.service.BookCoverService;
import fu.libmanagement.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Đổi ảnh bìa sách. URL nằm dưới /librarian nên chỉ Thủ thư và Admin vào được
 * (đã cấu hình trong WebConfig, không cần sửa gì thêm).
 */
@Controller
@RequiredArgsConstructor
public class BookCoverController {

    private final BookCoverService bookCoverService;

    @GetMapping("/librarian/books/{id}/cover")
    public String form(@PathVariable Long id, Model model) {
        model.addAttribute("book", bookCoverService.getBook(id));
        return "catalog/book-cover-form";
    }

    @PostMapping("/librarian/books/{id}/cover")
    public String upload(@PathVariable Long id,
                         @RequestParam("cover") MultipartFile cover,
                         Model model, RedirectAttributes redirectAttributes) {
        try {
            bookCoverService.updateCover(id, cover);
            redirectAttributes.addFlashAttribute("message", "Đã cập nhật ảnh bìa");
            return "redirect:/librarian/books/" + id + "/cover";
        } catch (BusinessException e) {
            model.addAttribute("book", bookCoverService.getBook(id));
            model.addAttribute("error", e.getMessage());
            return "catalog/book-cover-form";
        }
    }
}
