package fu.libmanagement.auth.dto;

import fu.libmanagement.common.enums.Role;
import fu.libmanagement.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;


@Getter
@RequiredArgsConstructor
public class SessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String KEY = "loginUser";

    private final Long id;
    private final String fullName;
    private final String email;
    private final Role role;
    private final String avatarUrl;

    public static SessionUser of(User user) {
        return new SessionUser(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), user.getAvatarUrl());
    }

    public static SessionUser get(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (SessionUser) session.getAttribute(KEY);
    }

    public boolean isStaff() {
        return role != Role.READER;
    }
}
