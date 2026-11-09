package fu.libmanagement.fine.repository;

import fu.libmanagement.common.enums.FineStatus;
import fu.libmanagement.common.enums.FineType;
import fu.libmanagement.fine.entity.Fine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface FineRepository extends JpaRepository<Fine, Long> {

    /**
     * Tìm khoản phạt theo phiếu mượn và loại phạt (phù hợp với ràng buộc duy nhất uq_fines_loan_type).
     */
    Optional<Fine> findByLoanIdAndFineType(Long loanId, FineType fineType);

    /**
     * Tìm kiếm và lọc danh sách khoản phạt cho thủ thư.
     */
    @Query(value = """
            SELECT f FROM Fine f
            JOIN f.reader r
            LEFT JOIN f.loan l
            LEFT JOIN l.book b
            WHERE (:status IS NULL OR f.status = :status)
              AND (
                :keyword = ''
                OR LOWER(r.fullName) LIKE :keyword
                OR LOWER(r.email) LIKE :keyword
                OR LOWER(b.title) LIKE :keyword
                OR (f.description IS NOT NULL AND LOWER(f.description) LIKE :keyword)
                OR CAST(f.id AS string) LIKE :keyword
                OR (l IS NOT NULL AND CAST(l.id AS string) LIKE :keyword)
              )
            """,
            countQuery = """
            SELECT COUNT(f) FROM Fine f
            JOIN f.reader r
            LEFT JOIN f.loan l
            LEFT JOIN l.book b
            WHERE (:status IS NULL OR f.status = :status)
              AND (
                :keyword = ''
                OR LOWER(r.fullName) LIKE :keyword
                OR LOWER(r.email) LIKE :keyword
                OR LOWER(b.title) LIKE :keyword
                OR (f.description IS NOT NULL AND LOWER(f.description) LIKE :keyword)
                OR CAST(f.id AS string) LIKE :keyword
                OR (l IS NOT NULL AND CAST(l.id AS string) LIKE :keyword)
              )
            """)
    Page<Fine> searchFines(
            @Param("keyword") String keyword,
            @Param("status") FineStatus status,
            Pageable pageable
    );

    long countByStatus(FineStatus status);

    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM Fine f WHERE f.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") FineStatus status);
}

