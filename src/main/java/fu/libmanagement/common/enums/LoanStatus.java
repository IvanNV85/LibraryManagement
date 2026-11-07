package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trạng thái yêu cầu mượn và phiếu mượn sách.
 * Đồng bộ chuẩn theo CHECK constraint của bảng loans trong Database:
 * PENDING, APPROVED, REJECTED, CANCELLED, EXPIRED, BORROWED, OVERDUE, RETURNED.
 */
@Getter
@RequiredArgsConstructor
public enum LoanStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt (Chờ nhận sách)"),
    REJECTED("Bị từ chối"),
    CANCELLED("Đã hủy"),
    EXPIRED("Hết hạn nhận sách"),
    BORROWED("Đang mượn"),
    OVERDUE("Quá hạn trả"),
    RETURNED("Đã trả");

    private final String label;
}
