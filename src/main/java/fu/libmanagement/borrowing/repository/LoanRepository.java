package fu.libmanagement.borrowing.repository;

import fu.libmanagement.borrowing.entity.Loan;
import fu.libmanagement.common.enums.LoanSource;
import fu.libmanagement.common.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    /** Lịch sử mượn của độc giả (nạp luôn sách để hiện tên và ảnh bìa). */
    @EntityGraph(attributePaths = "book")
    Page<Loan> findByReaderId(Long readerId, Pageable pageable);

    @EntityGraph(attributePaths = "book")
    Page<Loan> findByReaderIdAndStatus(Long readerId, LoanStatus status, Pageable pageable);

    Optional<Loan> findByIdAndReaderId(Long id, Long readerId);

    @EntityGraph(attributePaths = {"book", "reader"})
    @Query("SELECT l FROM Loan l WHERE l.id = :id")
    Optional<Loan> findWithDetailsById(@Param("id") Long id);

    long countByReaderIdAndStatusIn(Long readerId, Collection<LoanStatus> statuses);

    boolean existsByReaderIdAndStatus(Long readerId, LoanStatus status);

    boolean existsByReaderIdAndBookIdAndStatusIn(Long readerId, Long bookId, Collection<LoanStatus> statuses);

    Optional<Loan> findFirstByReaderIdAndBookIdAndStatusIn(Long readerId, Long bookId, Collection<LoanStatus> statuses);

    List<Loan> findByStatusInAndDueDateBefore(Collection<LoanStatus> statuses, LocalDate date);

    List<Loan> findByStatusAndDueDateBetween(LoanStatus status, LocalDate from, LocalDate to);

    List<Loan> findByStatusAndPickupDeadlineBefore(LoanStatus status, LocalDateTime time);

    long countByStatus(LoanStatus status);

    long countBySourceAndStatus(LoanSource source, LoanStatus status);

    /**
     * Tìm kiếm và lọc phiếu mượn / yêu cầu mượn dành cho Thủ thư.
     */
    @EntityGraph(attributePaths = {"book", "reader"})
    @Query(value = """
            SELECT l FROM Loan l
            JOIN l.reader r
            JOIN l.book b
            WHERE (:source IS NULL OR l.source = :source)
              AND (:status IS NULL OR l.status = :status)
              AND (
                :keyword = ''
                OR LOWER(r.fullName) LIKE :keyword
                OR LOWER(r.email) LIKE :keyword
                OR LOWER(b.title) LIKE :keyword
                OR LOWER(b.isbn) LIKE :keyword
                OR CAST(l.id AS string) LIKE :keyword
              )
            """,
            countQuery = """
            SELECT COUNT(l) FROM Loan l
            JOIN l.reader r
            JOIN l.book b
            WHERE (:source IS NULL OR l.source = :source)
              AND (:status IS NULL OR l.status = :status)
              AND (
                :keyword = ''
                OR LOWER(r.fullName) LIKE :keyword
                OR LOWER(r.email) LIKE :keyword
                OR LOWER(b.title) LIKE :keyword
                OR LOWER(b.isbn) LIKE :keyword
                OR CAST(l.id AS string) LIKE :keyword
              )
            """)
    Page<Loan> searchLibrarianLoans(
            @Param("keyword") String keyword,
            @Param("source") LoanSource source,
            @Param("status") LoanStatus status,
            Pageable pageable
    );
}