package fu.libmanagement.auth.controller;

import fu.libmanagement.auth.dto.RegisterForm;
import fu.libmanagement.auth.dto.SessionUser;
import fu.libmanagement.auth.service.AuthService;
import fu.libmanagement.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/* UC1 UC2*/
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ---------- Log In/Log Out ----------

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request) {
        if (SessionUser.get(request) != null) {
            return "redirect:/";
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                        HttpServletRequest request, Model model) {
        try {
            SessionUser user = authService.login(email, password);
            HttpSession session = request.getSession();
            request.changeSessionId(); // đổi id session sau khi đăng nhập để chống session fixation
            session.setAttribute(SessionUser.KEY, user);
            return "redirect:/";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            return "auth/login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }

    // ---------- Register ----------

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("form") RegisterForm form, Model model) {
        try {
            authService.register(form);
            return "redirect:/register/success"; // redirect để F5 không gửi lại form
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/register/success")
    public String registerSuccess() {
        return "auth/register-success";
    }

    // ---------- Email Verify ----------

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, Model model) {
        model.addAttribute("success", authService.verifyEmail(token));
        return "auth/verify-email";
    }

    @GetMapping("/resend-verification")
    public String resendPage() {
        return "auth/resend-verification";
    }

    @PostMapping("/resend-verification")
    public String resend(@RequestParam String email, Model model) {
        authService.resendVerification(email);
        model.addAttribute("message", "Nếu email tồn tại và chưa được xác thực, chúng tôi đã gửi lại liên kết xác thực.");
        return "auth/resend-verification";
    }
}
