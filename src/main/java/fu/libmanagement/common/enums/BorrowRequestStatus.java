package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trạng thái yêu cầu mượn sách trực tuyến (UC11, UC16).
 * Quy ước chung trong tài liệu phân công: PENDING, APPROVED, REJECTED, CANCELLED.
 */
@Getter
@RequiredArgsConstructor
public enum BorrowRequestStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Bị từ chối"),
    CANCELLED("Đã hủy");

    private final String label;
}

