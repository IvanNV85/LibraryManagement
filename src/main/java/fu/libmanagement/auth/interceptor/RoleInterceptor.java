package fu.libmanagement.auth.interceptor;

import fu.libmanagement.auth.dto.SessionUser;
import fu.libmanagement.common.enums.Role;
import fu.libmanagement.common.enums.AccountStatus;
import fu.libmanagement.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

/**
 * Chặn truy cập nếu chưa đăng nhập hoặc sai vai trò (UC2 - phân quyền).
 * Danh sách vai trò rỗng nghĩa là chỉ cần đã đăng nhập.
 * Đường dẫn nào dùng interceptor nào được khai báo trong WebConfig.
 */
public class RoleInterceptor implements HandlerInterceptor {

    private final Set<Role> allowedRoles;
    private final UserRepository userRepository;

    public RoleInterceptor(UserRepository userRepository, Role... allowedRoles) {
        this.userRepository = userRepository;
        this.allowedRoles = Set.of(allowedRoles);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        SessionUser user = SessionUser.get(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        // A locked or deleted account must not continue to use an existing session.
        if (userRepository.findById(user.getId()).filter(u -> u.getStatus() == AccountStatus.ACTIVE).isEmpty()) {
            request.getSession(false).invalidate();
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        if (!allowedRoles.isEmpty() && !allowedRoles.contains(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập trang này");
            return false;
        }
        return true;
    }
}
