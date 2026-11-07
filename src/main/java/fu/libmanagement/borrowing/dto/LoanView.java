package fu.libmanagement.borrowing.dto;

import fu.libmanagement.common.enums.LoanStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class LoanView {

    private final Long id;
    private final Long bookId;
    private final String bookTitle;
    private final String coverUrl;
    private final LoanStatus status;
    private final LocalDateTime requestedAt;
    private final LocalDateTime pickupDeadline;
    private final LocalDate dueDate;
    private final LocalDateTime returnedAt;
    private final int renewCount;
    private final int maxRenewals;
    private final String rejectReason;

    private final BigDecimal fineAmount;
    private final boolean fineUnpaid;

    private final boolean fineEstimated;
    private final boolean canRenew;
    private final boolean canCancel;

    private final String renewNote;
}