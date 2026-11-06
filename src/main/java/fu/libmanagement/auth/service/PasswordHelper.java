package fu.libmanagement.auth.service;

import fu.libmanagement.common.exception.BusinessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** Băm mật khẩu bằng BCrypt và kiểm tra mật khẩu mới. Dùng chung cho đăng ký và đổi mật khẩu. */
@Component
public class PasswordHelper {

    private static final int MIN_LENGTH = 6;
    private static final int MAX_BYTES = 72; // giới hạn của BCrypt (chữ có dấu tiếng Việt chiếm nhiều byte)

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String passwordHash) {
        return rawPassword != null && encoder.matches(rawPassword, passwordHash);
    }

    /** Kiểm tra mật khẩu mới: đủ dài và khớp với ô nhập lại. Sai thì ném BusinessException. */
    public void validateNewPassword(String password, String confirmPassword) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new BusinessException("Mật khẩu phải có ít nhất " + MIN_LENGTH + " ký tự");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new BusinessException("Mật khẩu quá dài (tối đa " + MAX_BYTES + " byte)");
        }
        if (!password.equals(confirmPassword)) {
            throw new BusinessException("Mật khẩu nhập lại không khớp");
        }
    }
}
