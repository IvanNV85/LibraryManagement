package fu.libmanagement.report.controller;

import fu.libmanagement.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reports;

    @GetMapping("/admin/reports")
    public String dashboard(Model model) {
        model.addAttribute("statistics", reports.statistics());
        return "admin/reports";
    }
}
