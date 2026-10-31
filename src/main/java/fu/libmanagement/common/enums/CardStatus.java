package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Trạng thái thẻ thư viện. Đăng ký xong thẻ ở trạng thái PENDING, chờ thủ thư duyệt (UC23). */
@Getter
@RequiredArgsConstructor
public enum CardStatus {
    PENDING("Chờ duyệt"),
    ACTIVE("Đang hoạt động"),
    LOCKED("Bị khóa");

    private final String label;
}
