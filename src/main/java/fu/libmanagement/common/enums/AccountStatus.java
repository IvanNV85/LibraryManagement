package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Trạng thái tài khoản đăng nhập (khóa/mở khóa bởi thủ thư hoặc admin). */
@Getter
@RequiredArgsConstructor
public enum AccountStatus {
    ACTIVE("Đang hoạt động"),
    LOCKED("Bị khóa");

    private final String label;
}
