package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trạng thái phiếu nhập sách (UC22 - Kho và quản trị).
 */
@Getter
@RequiredArgsConstructor
public enum PurchaseOrderStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Bị từ chối"),
    RECEIVED("Đã nhập kho"),
    CANCELLED("Đã hủy");

    private final String label;
}

