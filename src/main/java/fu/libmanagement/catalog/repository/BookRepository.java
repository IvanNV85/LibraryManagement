package fu.libmanagement.catalog.repository;

import fu.libmanagement.catalog.dto.CategoryCount;
import fu.libmanagement.catalog.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    /** Danh sách tất cả sách đang hiển thị (khi không nhập từ khóa). */
    Page<Book> findByVisibleTrue(Pageable pageable);

    /**
     * Tìm sách đang hiển thị theo từ khóa. {@code field} là một trong ALL, TITLE, AUTHOR, CATEGORY, ISBN;
     * {@code kw} đã ở dạng chữ thường và có dấu % hai đầu. Phân trang ngay trong DB.
     */
    @Query("""
            select b from Book b
            left join b.category c
            where b.visible = true
              and (   ((:field = 'ALL' or :field = 'TITLE') and lower(b.title) like :kw)
                   or ((:field = 'ALL' or :field = 'ISBN') and lower(b.isbn) like :kw)
                   or ((:field = 'ALL' or :field = 'CATEGORY') and lower(c.name) like :kw)
                   or ((:field = 'ALL' or :field = 'AUTHOR') and exists (
                           select 1 from b.authors a where lower(a.name) like :kw)))
            """)
    Page<Book> search(@Param("field") String field, @Param("kw") String kw, Pageable pageable);

    /** Gợi ý từ khóa: chỉ lấy cột tên sách (nhẹ hơn lấy cả entity). */
    @Query("select b.title from Book b where b.visible = true and lower(b.title) like :kw order by b.title")
    List<String> findTitleSuggestions(@Param("kw") String kw, Pageable pageable);

    /** 5 sách mới nhất đang hiển thị (cùng giờ thêm thì lấy cuốn có id lớn hơn). */
    List<Book> findTop5ByVisibleTrueOrderByCreatedAtDescIdDesc();

    /** Số đầu sách của từng thể loại (chỉ thể loại đang có sách), tính bằng 1 câu query. */
    @Query("""
            select new fu.libmanagement.catalog.dto.CategoryCount(c.name, count(b))
            from Book b join b.category c
            where b.visible = true
            group by c.id, c.name
            order by c.name
            """)
    List<CategoryCount> countBooksByCategory();
}
