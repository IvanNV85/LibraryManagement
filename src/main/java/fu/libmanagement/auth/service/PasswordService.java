package fu.libmanagement.auth.service;

import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.user.entity.User;
import fu.libmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC3 Đổi mật khẩu khi đã đăng nhập (độc giả và nhân viên đều dùng). */
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordHelper passwordHelper;

    /** Phải nhập đúng mật khẩu hiện tại; mật khẩu mới phải khác mật khẩu cũ. */
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
        user.setPasswordHash(passwordHelper.hash(newPassword)); // Hibernate tự UPDATE khi commit
    }
}
