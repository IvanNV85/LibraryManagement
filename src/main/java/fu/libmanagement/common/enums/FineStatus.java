package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trạng thái khoản phạt (UC12, UC19).
 */
@Getter
@RequiredArgsConstructor
public enum FineStatus {
    UNPAID("Chưa thanh toán"),
    PAID("Đã thu phạt"),
    WAIVED("Được miễn");

    private final String label;
}

