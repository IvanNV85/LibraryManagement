package fu.libmanagement.auth.service;

import fu.libmanagement.common.exception.BusinessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** Băm mật khẩu bằng BCrypt và kiểm tra độ mạnh mật khẩu. Dùng chung cho đăng ký, đặt lại và đổi mật khẩu. */
@Component
public class PasswordHelper {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_BYTES = 72; // giới hạn của BCrypt (chữ có dấu tiếng Việt chiếm nhiều byte)

    // Phải có: chữ thường, chữ HOA, chữ số và 1 ký tự đặc biệt (không phải chữ, số, khoảng trắng).
    // \p{Ll} \p{Lu} \p{L} \p{N} nhận cả chữ có dấu nên "ă" không bị tính nhầm là ký tự đặc biệt.
    private static final Pattern STRONG =
            Pattern.compile("(?=.*\\p{Ll})(?=.*\\p{Lu})(?=.*\\d)(?=.*[^\\p{L}\\p{N}\\s]).*");

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String passwordHash) {
        return rawPassword != null && encoder.matches(rawPassword, passwordHash);
    }

    /** Kiểm tra mật khẩu mới: đủ mạnh và khớp với ô nhập lại. Sai thì ném BusinessException. */
    public void validateNewPassword(String password, String confirmPassword) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new BusinessException("Mật khẩu phải có ít nhất " + MIN_LENGTH + " ký tự");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new BusinessException("Mật khẩu quá dài (tối đa " + MAX_BYTES + " byte)");
        }
        if (!STRONG.matcher(password).matches()) {
            throw new BusinessException("Mật khẩu phải có chữ hoa, chữ thường, chữ số và ký tự đặc biệt (ví dụ @, #, !)");
        }
        if (!password.equals(confirmPassword)) {
            throw new BusinessException("Mật khẩu nhập lại không khớp");
        }
    }
}
