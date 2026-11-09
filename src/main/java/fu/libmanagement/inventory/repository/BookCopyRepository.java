package fu.libmanagement.inventory.repository;

import fu.libmanagement.inventory.entity.BookCopy;
import fu.libmanagement.inventory.entity.CopyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    boolean existsByBarcode(String barcode);
    long countByStatus(CopyStatus status);

    @EntityGraph(attributePaths = "book")
    Optional<BookCopy> findByBarcode(String barcode);

    @EntityGraph(attributePaths = "book")
    java.util.List<BookCopy> findByBookIdAndStatus(Long bookId, CopyStatus status);

    @EntityGraph(attributePaths = "book")
    @Query("""
        select c from BookCopy c join c.book b
        where (:status is null or c.status = :status)
          and (lower(c.barcode) like :keyword or lower(b.title) like :keyword or lower(b.isbn) like :keyword)
        """)
    Page<BookCopy> search(@Param("keyword") String keyword, @Param("status") CopyStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "book")
    @Query("select c from BookCopy c where c.id = :id")
    Optional<BookCopy> findWithBook(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BookCopy c where c.id = :id")
    Optional<BookCopy> findForUpdate(@Param("id") Long id);

    @Query(value = "select count(*) from loans where copy_id = :id and status in ('APPROVED','BORROWED','OVERDUE')", nativeQuery = true)
    long countActiveLoans(@Param("id") Long id);

    @Query(value = "select count(*) from reservations where copy_id = :id and status = 'READY'", nativeQuery = true)
    long countActiveReservations(@Param("id") Long id);
}
