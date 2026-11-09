package fu.libmanagement.catalog.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Thông tin một cuốn sách trong danh sách kết quả tìm kiếm. */
@Getter
@RequiredArgsConstructor
public class BookSummary {

    private final Long id;
    private final String title;
    private final String isbn;
    private final String publisher;
    private final Integer publishYear;
    private final String coverUrl;
    private final String categoryName;
    private final String authorNames;
}
