package fu.libmanagement.user.dto;

import lombok.Getter;
import lombok.Setter;

/** Dữ liệu form cập nhật hồ sơ. Không có email và mã thẻ vì hai trường này không cho sửa. */
@Getter
@Setter
public class ProfileForm {

    private String fullName;
    private String phone;
    private String address;
}
