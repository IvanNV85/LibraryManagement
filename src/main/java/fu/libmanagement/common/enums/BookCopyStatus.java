package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trạng thái bản sao sách (từng cuốn vật lý trong kho).
 * Đồng bộ chuẩn theo CHECK constraint của bảng book_copies trong Database:
 * AVAILABLE, HELD, BORROWED, DAMAGED, LOST, DISPOSED.
 */
@Getter
@RequiredArgsConstructor
public enum BookCopyStatus {
    AVAILABLE("Sẵn sàng"),
    HELD("Đang giữ chỗ"),
    BORROWED("Đang mượn"),
    DAMAGED("Hư hỏng"),
    LOST("Làm mất"),
    DISPOSED("Đã thanh lý");

    private final String label;
}
