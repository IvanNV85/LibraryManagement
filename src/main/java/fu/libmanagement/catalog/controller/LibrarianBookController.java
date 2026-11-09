package fu.libmanagement.catalog.controller;

import fu.libmanagement.catalog.dto.BookForm;
import fu.libmanagement.catalog.entity.Book;
import fu.libmanagement.catalog.service.LibrarianBookService;
import fu.libmanagement.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller xử lý màn hình quản lý đầu sách cho Thủ thư và Admin (UC20).
 * Tiền tố /librarian/** được bảo vệ bởi RoleInterceptor.
 */
@Controller
@RequestMapping("/librarian/books")
@RequiredArgsConstructor
public class LibrarianBookController {

    private final LibrarianBookService librarianBookService;

    /**
     * Danh sách đầu sách cho thủ thư (có tìm kiếm và phân trang).
     */
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<Book> result = librarianBookService.getBooks(keyword, page);

        model.addAttribute("result", result);
        model.addAttribute("books", result.getContent());
        model.addAttribute("keyword", keyword.trim());
        return "librarian/books/list";
    }

    /**
     * Mở form thêm mới đầu sách.
     */
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("bookForm", new BookForm());
        model.addAttribute("categories", librarianBookService.getAllCategories());
        model.addAttribute("publishers", librarianBookService.getAllPublishers());
        model.addAttribute("isEdit", false);
        return "librarian/books/form";
    }

    /**
     * Xử lý thêm mới đầu sách.
     */
    @PostMapping("/new")
    public String create(@ModelAttribute("bookForm") BookForm form,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            librarianBookService.createBook(form);
            redirectAttributes.addFlashAttribute("message", "Thêm đầu sách mới thành công!");
            return "redirect:/librarian/books";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", librarianBookService.getAllCategories());
            model.addAttribute("publishers", librarianBookService.getAllPublishers());
            model.addAttribute("isEdit", false);
            return "librarian/books/form";
        }
    }

    /**
     * Mở form chỉnh sửa đầu sách.
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            BookForm form = librarianBookService.getBookForm(id);
            model.addAttribute("bookForm", form);
            model.addAttribute("categories", librarianBookService.getAllCategories());
            model.addAttribute("publishers", librarianBookService.getAllPublishers());
            model.addAttribute("isEdit", true);
            return "librarian/books/form";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/librarian/books";
        }
    }

    /**
     * Xử lý cập nhật đầu sách.
     */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute("bookForm") BookForm form,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            librarianBookService.updateBook(id, form);
            redirectAttributes.addFlashAttribute("message", "Cập nhật thông tin đầu sách thành công!");
            return "redirect:/librarian/books";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", librarianBookService.getAllCategories());
            model.addAttribute("publishers", librarianBookService.getAllPublishers());
            model.addAttribute("isEdit", true);
            return "librarian/books/form";
        }
    }

    /**
     * Bật/tắt trạng thái hiển thị của đầu sách (Soft-delete / Toggle visible).
     */
    @PostMapping("/{id}/toggle-visibility")
    public String toggleVisibility(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            librarianBookService.toggleVisibility(id);
            redirectAttributes.addFlashAttribute("message", "Đã cập nhật trạng thái hiển thị của đầu sách!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/librarian/books";
    }
}

