package fu.libmanagement.auth.controller;

import fu.libmanagement.auth.dto.SessionUser;
import fu.libmanagement.auth.service.PasswordService;
import fu.libmanagement.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/*UC3*/
@Controller
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordService passwordService;

    // ---------- Forget pass ----------

    @GetMapping("/forgot-password")
    public String forgotPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgot(@RequestParam String email, Model model) {
        passwordService.requestReset(email);
        model.addAttribute("message", "Nếu email tồn tại trong hệ thống, chúng tôi đã gửi liên kết đặt lại mật khẩu.");
        return "auth/forgot-password";
    }

    // ---------- Reset Pass in Mail ----------

    @GetMapping("/reset-password")
    public String resetPage(@RequestParam String token, Model model) {
        if (passwordService.isResetTokenValid(token)) {
            model.addAttribute("token", token);
        } else {
            model.addAttribute("invalid", true);
        }
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String reset(@RequestParam String token, @RequestParam String password,
                        @RequestParam String confirmPassword,
                        Model model, RedirectAttributes redirectAttributes) {
        try {
            passwordService.resetPassword(token, password, confirmPassword);
            redirectAttributes.addFlashAttribute("message", "Đặt lại mật khẩu thành công, hãy đăng nhập bằng mật khẩu mới.");
            return "redirect:/login";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("token", token);
            return "auth/reset-password";
        }
    }

    // ---------- Change Pass ----------

    @GetMapping("/account/change-password")
    public String changePage() {
        return "auth/change-password";
    }

    @PostMapping("/account/change-password")
    public String change(@RequestParam String currentPassword, @RequestParam String newPassword,
                         @RequestParam String confirmPassword,
                         HttpServletRequest request, Model model) {
        SessionUser user = SessionUser.get(request);
        try {
            passwordService.changePassword(user.getId(), currentPassword, newPassword, confirmPassword);
            model.addAttribute("message", "Đổi mật khẩu thành công");
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "auth/change-password";
    }
}
