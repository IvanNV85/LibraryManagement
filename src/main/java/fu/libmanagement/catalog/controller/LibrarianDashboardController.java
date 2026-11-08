package fu.libmanagement.catalog.controller;

import fu.libmanagement.catalog.entity.Book;
import fu.libmanagement.catalog.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Controller màn hình Dashboard tổng quan dành cho Thủ thư và Admin.
 */
@Controller
@RequestMapping("/librarian")
@RequiredArgsConstructor
public class LibrarianDashboardController {

    private final BookRepository bookRepository;

    @GetMapping({"", "/dashboard"})
    public String dashboard(Model model) {
        long totalBooks = bookRepository.count();
        long visibleBooks = bookRepository.countByVisibleTrue();
        long hiddenBooks = bookRepository.countByVisibleFalse();
        List<Book> recentBooks = bookRepository.findTop5ByVisibleTrueOrderByCreatedAtDescIdDesc();

        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("visibleBooks", visibleBooks);
        model.addAttribute("hiddenBooks", hiddenBooks);
        model.addAttribute("recentBooks", recentBooks);

        return "librarian/dashboard";
    }
}

