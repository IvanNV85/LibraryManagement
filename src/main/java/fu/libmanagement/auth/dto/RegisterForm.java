package fu.libmanagement.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterForm {

    private String fullName;
    private String email;
    private String phone;
    private String password;
    private String confirmPassword;
}
