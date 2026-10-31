package fu.libmanagement.catalog.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Một thể loại kèm số đầu sách đang hiển thị (dùng cho phần "Duyệt theo thể loại" ở trang chủ). */
@Getter
@RequiredArgsConstructor
public class CategoryCount {

    private final String name;
    private final Long bookCount;
}
