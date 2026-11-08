package fu.libmanagement.engagement.repository;

import fu.libmanagement.engagement.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    //Số đánh giá 1 sách
    long countByBookId(Long bookId);

    // Các đánh giá mới nhất
    List<Review> findByBookIdOrderByCreatedAtDesc(Long bookId, Pageable pageable);

    //Độc giả đánh giá sách
    Optional<Review> findByBookIdAndReaderId(Long bookId, Long readerId);

    @Query("select avg(r.rating) from Review r where r.book.id = :bookId")
    Double averageRating(@Param("bookId") Long bookId);

    @Query(value = "select count(*) from loans where reader_id = :readerId and book_id = :bookId "
            + "and status in ('BORROWED', 'OVERDUE', 'RETURNED')", nativeQuery = true)
    int countBorrowed(@Param("readerId") Long readerId, @Param("bookId") Long bookId);
}
