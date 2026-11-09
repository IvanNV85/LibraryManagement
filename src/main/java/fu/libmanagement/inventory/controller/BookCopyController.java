package fu.libmanagement.inventory.controller;

import fu.libmanagement.inventory.entity.CopyStatus;
import fu.libmanagement.inventory.service.BookCopyService;
import fu.libmanagement.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;

@Controller
@RequestMapping("/librarian/copies")
@RequiredArgsConstructor
public class BookCopyController {
    private final BookCopyService service;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "ALL") String status,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("result", service.search(keyword, status, page));
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("statuses", CopyStatus.values());
        return "librarian/copy-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("books", service.allBooks());
        return "librarian/copy-new";
    }

    @PostMapping
    public String create(@RequestParam(required = false) Long bookId, @RequestParam String barcode,
                         @RequestParam(required = false) String shelfLocation,
                         @RequestParam(required = false) BigDecimal price,
                         @RequestParam(required = false) String note, RedirectAttributes redirect) {
        try {
            Long id = service.create(bookId, barcode, shelfLocation, price, note);
            redirect.addFlashAttribute("message", "Đã thêm bản sao.");
            return "redirect:/librarian/copies/" + id;
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addFlashAttribute("bookId", bookId);
            redirect.addFlashAttribute("barcode", barcode);
            redirect.addFlashAttribute("shelfLocation", shelfLocation);
            redirect.addFlashAttribute("price", price);
            redirect.addFlashAttribute("note", note);
            return "redirect:/librarian/copies/new";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("copy", service.detail(id));
        return "librarian/copy-detail";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @RequestParam(required = false) String shelfLocation,
                         @RequestParam(required = false) BigDecimal price,
                         @RequestParam(required = false) String note, RedirectAttributes redirect) {
        try { service.update(id, shelfLocation, price, note); redirect.addFlashAttribute("message", "Đã cập nhật bản sao."); }
        catch (BusinessException e) { redirect.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/librarian/copies/" + id;
    }

    @PostMapping("/{id}/condition")
    public String condition(@PathVariable Long id, @RequestParam String status, RedirectAttributes redirect) {
        try { service.recordCondition(id, status); redirect.addFlashAttribute("message", "Đã ghi nhận tình trạng bản sao."); }
        catch (BusinessException e) { redirect.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/librarian/copies/" + id;
    }
}
