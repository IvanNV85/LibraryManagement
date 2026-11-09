package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Loại tiền phạt (UC19).
 */
@Getter
@RequiredArgsConstructor
public enum FineType {
    OVERDUE("Trả trễ hạn"),
    DAMAGED("Làm hư hỏng sách"),
    LOST("Làm mất sách");

    private final String label;
}

