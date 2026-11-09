package fu.libmanagement.inventory.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CopyStatus {
    AVAILABLE("Sẵn sàng", "bg-success-subtle text-success border border-success-subtle", "bi-check-circle"),
    HELD("Đang giữ", "bg-primary-subtle text-primary border border-primary-subtle", "bi-bookmark-check"),
    BORROWED("Đang mượn", "bg-warning-subtle text-warning-emphasis border border-warning-subtle", "bi-book-half"),
    DAMAGED("Bị hỏng", "bg-danger-subtle text-danger border border-danger-subtle", "bi-exclamation-triangle"),
    LOST("Mất sách", "bg-danger text-white", "bi-x-octagon"),
    DISPOSED("Đã thanh lý", "bg-secondary-subtle text-secondary border border-secondary-subtle", "bi-trash");

    private final String label;
    private final String badgeClass;
    private final String iconClass;
}
