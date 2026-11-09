package fu.libmanagement.notification.controller;

import fu.libmanagement.auth.dto.SessionUser;
import fu.libmanagement.notification.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;


@ControllerAdvice
@RequiredArgsConstructor
public class NotificationAdvice {

    private final NotificationService notificationService;

    @ModelAttribute("unreadNotifications")
    public long unreadNotifications(HttpServletRequest request) {
        SessionUser user = SessionUser.get(request);
        if (user == null || !user.isReader()) {
            return 0;
        }
        try {
            return notificationService.countUnread(user.getId());
        } catch (RuntimeException e) {
            return 0;
        }
    }
}