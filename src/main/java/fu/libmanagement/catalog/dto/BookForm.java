package fu.libmanagement.catalog.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Dữ liệu form thêm mới / cập nhật đầu sách (UC20).
 */
@Getter
@Setter
public class BookForm {

    private Long id;
    private String isbn;
    private String title;
    private String publisher;
    private Integer publishYear;
    private Long categoryId;
    private String authorNames;
    private String coverUrl;
    private String description;
    private boolean visible = true;
}

