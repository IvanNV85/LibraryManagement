package fu.libmanagement.catalog.controller;

import fu.libmanagement.catalog.dto.BookSummary;
import fu.libmanagement.catalog.dto.SearchField;
import fu.libmanagement.catalog.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/** UC5 Tìm kiếm sách. Khách và độc giả đều dùng được (URL công khai). */
@Controller
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping("/books")
    public String search(@RequestParam(defaultValue = "") String keyword,
                         @RequestParam(defaultValue = "ALL") String field,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        SearchField searchField = SearchField.from(field);
        Page<BookSummary> result = bookService.search(keyword, searchField, page);

        model.addAttribute("result", result);
        model.addAttribute("books", result.getContent());
        model.addAttribute("keyword", keyword.trim());
        model.addAttribute("field", searchField.name());
        model.addAttribute("fields", SearchField.values());
        return "catalog/book-list";
    }

    /** Gợi ý từ khóa khi gõ. Trả về văn bản thuần, mỗi dòng một gợi ý (đơn giản hơn JSON). */
    @GetMapping(value = "/books/suggest", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String suggest(@RequestParam(defaultValue = "") String q) {
        List<String> titles = bookService.suggest(q);
        return String.join("\n", titles);
    }
}
