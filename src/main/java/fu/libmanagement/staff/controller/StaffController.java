package fu.libmanagement.staff.controller;

import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.staff.dto.StaffForm;
import fu.libmanagement.user.entity.User;
import fu.libmanagement.staff.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/staff")
@RequiredArgsConstructor
public class StaffController {
    private final StaffService service;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("keyword", keyword.trim());
        model.addAttribute("result", service.search(keyword, page));
        return "admin/staff-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new StaffForm());
        model.addAttribute("creating", true);
        return "admin/staff-form";
    }

    @PostMapping
    public String create(@ModelAttribute("form") StaffForm form, Model model, RedirectAttributes redirect) {
        try {
            User user = service.create(form);
            redirect.addFlashAttribute("message", "Đã tạo tài khoản thủ thư");
            return "redirect:/admin/staff/" + user.getId();
        } catch (BusinessException e) {
            form.setPassword(null); form.setConfirmPassword(null);
            model.addAttribute("error", e.getMessage()); model.addAttribute("creating", true);
            return "admin/staff-form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("staff", service.get(id));
        return "admin/staff-detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User u = service.get(id);
        StaffForm form = new StaffForm();
        form.setFullName(u.getFullName()); form.setEmail(u.getEmail()); form.setPhone(u.getPhone()); form.setAddress(u.getAddress());
        model.addAttribute("form", form); model.addAttribute("staffId", id); model.addAttribute("creating", false);
        return "admin/staff-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("form") StaffForm form, Model model, RedirectAttributes redirect) {
        try {
            service.update(id, form); redirect.addFlashAttribute("message", "Đã cập nhật nhân viên");
            return "redirect:/admin/staff/" + id;
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage()); model.addAttribute("staffId", id); model.addAttribute("creating", false);
            return "admin/staff-form";
        }
    }

    @PostMapping("/{id}/lock")
    public String lock(@PathVariable Long id, RedirectAttributes redirect) { return changeStatus(id, true, redirect); }
    @PostMapping("/{id}/unlock")
    public String unlock(@PathVariable Long id, RedirectAttributes redirect) { return changeStatus(id, false, redirect); }
    private String changeStatus(Long id, boolean locked, RedirectAttributes redirect) {
        try { service.setLocked(id, locked); redirect.addFlashAttribute("message", locked ? "Đã khóa tài khoản" : "Đã mở khóa tài khoản"); }
        catch (BusinessException e) { redirect.addFlashAttribute("error", e.getMessage()); return "redirect:/admin/staff"; }
        return "redirect:/admin/staff/" + id;
    }
}
