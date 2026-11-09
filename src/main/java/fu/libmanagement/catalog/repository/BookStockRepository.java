package fu.libmanagement.catalog.repository;

import fu.libmanagement.catalog.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookStockRepository extends JpaRepository<Book, Long> {

    //Tổng số bản sách
    @Query(value = """
            select count(*) from book_copies 
            where book_id = :bookId 
            and status not in ('LOST', 'DISPOSED', 'LIQUIDATED')
            """, nativeQuery = true)
    int countCopies(@Param("bookId") Long bookId);

    //Số bản có thể cho mượn
    @Query(value = """
            select count(*) from book_copies 
            where book_id = :bookId 
            and status = 'AVAILABLE'
            """, nativeQuery = true)
    int countAvailable(@Param("bookId") Long bookId);

    //Các vị trí kệ
    @Query(value = """
            select distinct shelf_location from book_copies 
            where book_id = :bookId 
            and shelf_location is not null 
            and status not in ('LOST', 'DISPOSED', 'LIQUIDATED')
            """, nativeQuery = true)
    List<String> findShelves(@Param("bookId") Long bookId);
}
