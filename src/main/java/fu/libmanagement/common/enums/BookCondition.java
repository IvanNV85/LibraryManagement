package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tình trạng sách khi độc giả trả sách tại quầy (UC18: Tốt / Hỏng / Mất).
 */
@Getter
@RequiredArgsConstructor
public enum BookCondition {
    GOOD("Tốt"),
    DAMAGED("Hư hỏng"),
    LOST("Mất sách");

    private final String label;
}

