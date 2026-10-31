package fu.libmanagement.auth.service;

import fu.libmanagement.auth.entity.AuthToken;
import fu.libmanagement.auth.entity.TokenType;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.common.util.MailService;
import fu.libmanagement.common.util.ValidationUtil;
import fu.libmanagement.user.entity.User;
import fu.libmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/** UC3 Quên mật khẩu (đặt lại bằng liên kết email) và đổi mật khẩu khi đã đăng nhập. */
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordHelper passwordHelper;
    private final MailService mailService;

    @Value("${app.auth.reset-token-minutes}")
    private int resetTokenMinutes;

    /** Gửi liên kết đặt lại mật khẩu. Không báo email có tồn tại hay không để tránh dò tài khoản. */
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(ValidationUtil.normalizeEmail(email)).ifPresent(user -> {
            String token = tokenService.createToken(user, TokenType.PASSWORD_RESET, Duration.ofMinutes(resetTokenMinutes));
            mailService.sendLink(user.getEmail(), "Đặt lại mật khẩu tài khoản thư viện",
                    "Xin chào " + user.getFullName() + ",\n\n"
                            + "Bấm vào liên kết dưới đây để đặt lại mật khẩu (có hiệu lực "
                            + resetTokenMinutes + " phút):",
                    "/reset-password", token,
                    "Nếu bạn không yêu cầu, hãy bỏ qua email này.");
        });
    }

    @Transactional(readOnly = true)
    public boolean isResetTokenValid(String token) {
        return tokenService.findValid(token, TokenType.PASSWORD_RESET).isPresent();
    }

    /** Đặt mật khẩu mới bằng token. Đặt lại xong thì gỡ luôn khóa tạm do đăng nhập sai. */
    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        AuthToken authToken = tokenService.findValid(token, TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new BusinessException("Liên kết không hợp lệ hoặc đã hết hạn"));
        passwordHelper.validateNewPassword(newPassword, confirmPassword);

        User user = authToken.getUser();
        user.setPasswordHash(passwordHelper.hash(newPassword));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        tokenService.delete(authToken);
    }

    /** Đổi mật khẩu khi đã đăng nhập (phải nhập đúng mật khẩu hiện tại). */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản"));
        if (!passwordHelper.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng");
        }
        passwordHelper.validateNewPassword(newPassword, confirmPassword);
        if (newPassword.equals(currentPassword)) {
            throw new BusinessException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.setPasswordHash(passwordHelper.hash(newPassword));
    }
}
