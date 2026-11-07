package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Phương thức thanh toán tiền phạt.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    ONLINE("Trực tuyến"),
    COUNTER("Tại quầy");

    private final String label;
}

