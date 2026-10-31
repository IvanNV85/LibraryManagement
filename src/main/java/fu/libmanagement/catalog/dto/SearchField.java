package fu.libmanagement.catalog.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Tiêu chí tìm kiếm sách: tìm theo tên, tác giả, thể loại, ISBN hoặc tất cả. */
@Getter
@RequiredArgsConstructor
public enum SearchField {
    ALL("Tất cả"),
    TITLE("Tên sách"),
    AUTHOR("Tác giả"),
    CATEGORY("Thể loại"),
    ISBN("ISBN");

    private final String label;

    /** Giá trị lạ hoặc rỗng thì dùng ALL. */
    public static SearchField from(String value) {
        for (SearchField field : values()) {
            if (field.name().equalsIgnoreCase(value)) {
                return field;
            }
        }
        return ALL;
    }
}
