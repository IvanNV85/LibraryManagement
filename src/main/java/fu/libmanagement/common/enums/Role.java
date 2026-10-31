package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Vai trò của tài khoản. Khách (Guest) là người chưa đăng nhập nên không lưu trong DB. */
@Getter
@RequiredArgsConstructor
public enum Role {
    READER("Độc giả"),
    LIBRARIAN("Thủ thư"),
    ADMIN("Quản trị viên");

    private final String label;
}
