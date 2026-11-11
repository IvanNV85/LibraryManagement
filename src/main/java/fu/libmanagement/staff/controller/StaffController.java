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

/** Quản lý tài khoản thủ thư; các URL /admin/** được RoleInterceptor giới hạn cho Admin. */
@Controller
@RequestMapping("/admin/staff")
@RequiredArgsConstructor
public class StaffController {
    private final StaffService service;

    /** Hiển thị danh sách nhân viên theo từ khóa và trang được chọn, giữ từ khóa trên ô tìm kiếm. */
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("keyword", keyword.trim());
        model.addAttribute("result", service.search(keyword, page));
        return "admin/staff-list";
    }

    /** Mở form thêm thủ thư; creating = true để giao diện hiển thị các ô nhập mật khẩu. */
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new StaffForm());
        model.addAttribute("creating", true);
        return "admin/staff-form";
    }

    /**
     * Nhận form tạo thủ thư và chuyển đến trang chi tiết khi lưu thành công.
     * Nếu lỗi nghiệp vụ, trả lại form cùng thông báo và yêu cầu nhập lại mật khẩu.
     */
    @PostMapping
    public String create(@ModelAttribute("form") StaffForm form, Model model, RedirectAttributes redirect) {
        try {
            User user = service.create(form);
            redirect.addFlashAttribute("message", "Đã tạo tài khoản thủ thư");
            return "redirect:/admin/staff/" + user.getId();
        } catch (BusinessException e) {
            // Giữ thông tin cá nhân đã nhập, xóa mật khẩu khỏi đối tượng trả về giao diện.
            form.setPassword(null); form.setConfirmPassword(null);
            model.addAttribute("error", e.getMessage()); model.addAttribute("creating", true);
            return "admin/staff-form";
        }
    }

    /** Hiển thị chi tiết tài khoản thủ thư; service kiểm tra cả ID và vai trò LIBRARIAN. */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("staff", service.get(id));
        return "admin/staff-detail";
    }

    /** Nạp thông tin cá nhân vào form sửa; creating = false để ẩn phần mật khẩu. */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User u = service.get(id);
        StaffForm form = new StaffForm();
        form.setFullName(u.getFullName()); form.setEmail(u.getEmail()); form.setPhone(u.getPhone()); form.setAddress(u.getAddress());
        model.addAttribute("form", form); model.addAttribute("staffId", id); model.addAttribute("creating", false);
        return "admin/staff-form";
    }

    /** Lưu thông tin chỉnh sửa; nếu lỗi nghiệp vụ thì giữ dữ liệu và hiển thị lại form sửa. */
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

    /** Khóa tài khoản thủ thư bằng cách chuyển trạng thái sang LOCKED. */
    @PostMapping("/{id}/lock")
    public String lock(@PathVariable Long id, RedirectAttributes redirect) { return changeStatus(id, true, redirect); }
    /** Mở khóa tài khoản thủ thư bằng cách chuyển trạng thái về ACTIVE. */
    @PostMapping("/{id}/unlock")
    public String unlock(@PathVariable Long id, RedirectAttributes redirect) { return changeStatus(id, false, redirect); }
    /**
     * Xử lý chung thao tác khóa/mở khóa và gửi thông báo qua flash attribute.
     * Thành công chuyển về chi tiết; lỗi nghiệp vụ chuyển về danh sách nhân viên.
     */
    private String changeStatus(Long id, boolean locked, RedirectAttributes redirect) {
        try { service.setLocked(id, locked); redirect.addFlashAttribute("message", locked ? "Đã khóa tài khoản" : "Đã mở khóa tài khoản"); }
        catch (BusinessException e) { redirect.addFlashAttribute("error", e.getMessage()); return "redirect:/admin/staff"; }
        return "redirect:/admin/staff/" + id;
    }
}
