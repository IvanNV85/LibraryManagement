package fu.libmanagement.setting.controller;

import fu.libmanagement.auth.dto.SessionUser;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.setting.service.SettingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
public class SettingController {
    private final SettingService service;

    @GetMapping
    public String page(Model model) {
        model.addAttribute("rules", service.currentRules());
        return "admin/settings";
    }

    @PostMapping
    public String update(@RequestParam String maxBooks, @RequestParam String loanDays, @RequestParam String finePerDay,
                         HttpServletRequest request, RedirectAttributes redirect) {
        try {
            service.updateRules(maxBooks, loanDays, finePerDay, SessionUser.get(request).getId());
            redirect.addFlashAttribute("message", "Đã cập nhật quy định");
        } catch (BusinessException e) { redirect.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/settings";
    }
}
