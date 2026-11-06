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

/** UC3 Đổi mật khẩu. URL nằm dưới /account nên interceptor bắt buộc phải đăng nhập (mọi vai trò). */
@Controller
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordService passwordService;

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
