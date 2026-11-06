package fu.libmanagement.auth.dto;

import lombok.Getter;
import lombok.Setter;

/** Dữ liệu form đăng ký tài khoản độc giả (UC1): họ tên, email, mật khẩu. */
@Getter
@Setter
public class RegisterForm {

    private String fullName;
    private String email;
    private String password;
    private String confirmPassword;
}
