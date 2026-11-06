package fu.libmanagement.engagement.repository;

import fu.libmanagement.engagement.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface ReviewRepository extends JpaRepository<Review, Long> {


    @Query("select avg(r.rating) from Review r where r.book.id = :bookId")
    Double averageRating(@Param("bookId") Long bookId);

    long countByBookId(Long bookId);

    @Query("select r from Review r join fetch r.reader where r.book.id = :bookId order by r.createdAt desc")
    List<Review> findLatest(@Param("bookId") Long bookId, Pageable pageable);

    //Đánh giá của 1 độc giả cho 1 sách
    Optional<Review> findByBookIdAndReaderId(Long bookId, Long readerId);

    @Query(value = "select count(*) from loans where reader_id = :readerId and book_id = :bookId "
            + "and status in ('BORROWED', 'OVERDUE', 'RETURNED')", nativeQuery = true)
    int countBorrowed(@Param("readerId") Long readerId, @Param("bookId") Long bookId);
}
