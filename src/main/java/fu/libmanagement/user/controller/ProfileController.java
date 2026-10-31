package fu.libmanagement.user.controller;

import fu.libmanagement.auth.dto.SessionUser;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.user.dto.ProfileForm;
import fu.libmanagement.user.entity.User;
import fu.libmanagement.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** UC4 Cập nhật hồ sơ cá nhân. URL nằm dưới /reader nên chỉ độc giả đã đăng nhập mới vào được. */
@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping("/reader/profile")
    public String profilePage(HttpServletRequest request, Model model) {
        User user = userService.getUser(SessionUser.get(request).getId());

        ProfileForm form = new ProfileForm();
        form.setFullName(user.getFullName());
        form.setPhone(user.getPhone());
        form.setAddress(user.getAddress());
        model.addAttribute("form", form);
        addReadOnlyInfo(model, user);
        return "user/profile";
    }

    @PostMapping("/reader/profile")
    public String updateProfile(@ModelAttribute("form") ProfileForm form,
                                @RequestParam(value = "avatar", required = false) MultipartFile avatar,
                                HttpServletRequest request, Model model,
                                RedirectAttributes redirectAttributes) {
        Long userId = SessionUser.get(request).getId();
        try {
            User user = userService.updateProfile(userId, form, avatar);
            // Cập nhật session để tên và ảnh trên thanh menu đổi theo
            request.getSession().setAttribute(SessionUser.KEY, SessionUser.of(user));
            redirectAttributes.addFlashAttribute("message", "Cập nhật hồ sơ thành công");
            return "redirect:/reader/profile";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            addReadOnlyInfo(model, userService.getUser(userId));
            return "user/profile";
        }
    }

    /** Thông tin chỉ để xem: email, ảnh hiện tại, thẻ thư viện. */
    private void addReadOnlyInfo(Model model, User user) {
        model.addAttribute("user", user);
        model.addAttribute("card", userService.getCard(user.getId()));
    }
}
