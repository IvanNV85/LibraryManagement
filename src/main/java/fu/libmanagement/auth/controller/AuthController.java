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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** UC1 Đăng ký, UC2 Đăng nhập / Đăng xuất. */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ---------- Đăng nhập / Đăng xuất ----------

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request) {
        SessionUser user = SessionUser.get(request);
        if (user != null) {
            if (user.isStaff()) {
                return "redirect:/librarian/dashboard";
            }
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
            if (user.isStaff()) {
                return "redirect:/librarian/dashboard";
            }
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

    // ---------- Đăng ký ----------

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("form") RegisterForm form, Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            authService.register(form);
            redirectAttributes.addFlashAttribute("message",
                    "Đăng ký thành công. Thẻ thư viện của bạn đang chờ thủ thư duyệt, bạn có thể đăng nhập ngay.");
            return "redirect:/login"; // redirect để F5 không gửi lại form
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }
}
