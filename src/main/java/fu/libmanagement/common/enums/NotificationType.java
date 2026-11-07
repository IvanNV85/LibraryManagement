package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Loại thông báo trong web cho độc giả và người dùng (UC15).
 */
@Getter
@RequiredArgsConstructor
public enum NotificationType {
    DUE_SOON("Sắp đến hạn trả sách"),
    OVERDUE("Sách quá hạn"),
    LOAN_APPROVED("Yêu cầu mượn được duyệt"),
    LOAN_REJECTED("Yêu cầu mượn bị từ chối"),
    RESERVATION_READY("Sách đặt trước đã có"),
    NEW_BOOK("Sách mới"),
    FINE_CREATED("Phát sinh tiền phạt"),
    SYSTEM("Thông báo hệ thống");

    private final String label;
}

