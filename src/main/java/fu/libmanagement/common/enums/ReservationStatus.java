package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trạng thái yêu cầu đặt trước khi sách đang hết (UC13).
 */
@Getter
@RequiredArgsConstructor
public enum ReservationStatus {
    WAITING("Chờ có sách"),
    READY("Sách đã sẵn sàng"),
    FULFILLED("Đã nhận sách"),
    CANCELLED("Đã hủy"),
    EXPIRED("Quá hạn nhận");

    private final String label;
}

