package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trạng thái giao dịch thanh toán.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    PENDING("Chờ xử lý"),
    SUCCESS("Thành công"),
    FAILED("Thất bại");

    private final String label;
}

