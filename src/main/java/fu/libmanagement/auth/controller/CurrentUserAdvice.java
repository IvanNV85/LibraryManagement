package fu.libmanagement.auth.controller;

import fu.libmanagement.auth.dto.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserAdvice {

    @ModelAttribute("currentUser")
    public SessionUser currentUser(HttpServletRequest request) {
        return SessionUser.get(request);
    }
}
