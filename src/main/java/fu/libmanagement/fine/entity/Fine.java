package fu.libmanagement.fine.entity;

import fu.libmanagement.borrowing.entity.Loan;
import fu.libmanagement.common.enums.FineStatus;
import fu.libmanagement.common.enums.FineType;
import fu.libmanagement.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Khoản tiền phạt (UC12, UC19).
 * Phát sinh khi độc giả trả trễ hạn, làm hư hỏng, hoặc làm mất sách.
 */
@Entity
@Table(name = "fines")
@Getter
@Setter
@NoArgsConstructor
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reader_id", nullable = false)
    private User reader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @Enumerated(EnumType.STRING)
    @Column(name = "fine_type", nullable = false, length = 20)
    private FineType fineType = FineType.OVERDUE;

    @Column(name = "overdue_days")
    private Integer overdueDays;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "note", length = 255)
    private String note;

    public String getDescription() {
        return note;
    }

    public void setDescription(String description) {
        this.note = description;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FineStatus status = FineStatus.UNPAID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}

