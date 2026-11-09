package fu.libmanagement.staff.dto;

import lombok.Getter;
import lombok.Setter;

/** Không nhận role hoặc status từ form. */
@Getter @Setter
public class StaffForm {
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String password;
    private String confirmPassword;
}
