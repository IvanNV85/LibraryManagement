package fu.libmanagement.catalog.controller;

import fu.libmanagement.catalog.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Trang chủ: giới thiệu, duyệt theo thể loại và sách mới. Công khai, ai cũng xem được. */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final BookService bookService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("categories", bookService.getCategoryCounts());
        model.addAttribute("newBooks", bookService.getNewestBooks());
        return "catalog/home";
    }
}
