package fu.libmanagement.auth.dto;

import fu.libmanagement.common.enums.Role;
import fu.libmanagement.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

/**
 * Thông tin người đăng nhập lưu trong HttpSession.
 * Các module khác lấy người dùng hiện tại bằng {@code SessionUser.get(request)}
 * (trả về null nếu chưa đăng nhập). Trong Thymeleaf dùng biến {@code currentUser}.
 */
@Getter
@RequiredArgsConstructor
public class SessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String KEY = "loginUser";

    private final Long id;
    private final String fullName;
    private final String email;
    private final Role role;

    public static SessionUser of(User user) {
        return new SessionUser(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    /** Không tạo session mới nếu khách chưa có session. */
    public static SessionUser get(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (SessionUser) session.getAttribute(KEY);
    }

    public boolean isReader() {
        return role == Role.READER;
    }

    public boolean isLibrarian() {
        return role == Role.LIBRARIAN;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /** Thủ thư hoặc admin. */
    public boolean isStaff() {
        return role != Role.READER;
    }
}
