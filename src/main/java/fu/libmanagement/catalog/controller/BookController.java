package fu.libmanagement.catalog.controller;

import fu.libmanagement.catalog.dto.BookSummary;
import fu.libmanagement.catalog.dto.SearchField;
import fu.libmanagement.catalog.entity.BookSort;
import fu.libmanagement.catalog.service.BookFilterService;
import fu.libmanagement.catalog.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/** UC5 Tìm kiếm sách theo tên, tác giả, ISBN, có phân trang. Khách và độc giả đều dùng được (URL công khai). */
@Controller
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final BookFilterService bookFilterService;

    @GetMapping("/books")
    public String search(@RequestParam(defaultValue = "") String keyword,
                         @RequestParam(defaultValue = "ALL") String field,
                         @RequestParam(name = "category", defaultValue = "0") long categoryId,
                         @RequestParam(defaultValue = "0") int year,
                         @RequestParam(defaultValue = "TITLE") BookSort sort,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        SearchField searchField = SearchField.from(field);
        Page<BookSummary> result = bookService.search(keyword, searchField, page);

        model.addAttribute("result", result);
        model.addAttribute("books", result.getContent());
        model.addAttribute("keyword", keyword.trim());
        model.addAttribute("field", searchField.name());
        model.addAttribute("fields", SearchField.values());

        model.addAttribute("categoryId", categoryId);
        model.addAttribute("year", year);
        model.addAttribute("sort", sort.name());
        model.addAttribute("categories", bookFilterService.getCategories());
        model.addAttribute("years", bookFilterService.getYears());
        return "catalog/book-list";
    }

    /**
     * Gợi ý từ khóa khi gõ. Trả về văn bản thuần, mỗi dòng một gợi ý (đơn giản hơn JSON).
     */
    @GetMapping(value = "/books/suggest", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String suggest(@RequestParam(defaultValue = "") String q) {
        List<String> titles = bookService.suggest(q);
        return String.join("\n", titles);
    }
}
