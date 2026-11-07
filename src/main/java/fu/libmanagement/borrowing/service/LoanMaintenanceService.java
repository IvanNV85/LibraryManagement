package fu.libmanagement.borrowing.service;

import fu.libmanagement.borrowing.entity.Loan;
import fu.libmanagement.borrowing.repository.BorrowingQueries;
import fu.libmanagement.borrowing.repository.LoanRepository;
import fu.libmanagement.common.enums.LoanStatus;
import fu.libmanagement.common.enums.NotificationType;
import fu.libmanagement.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanMaintenanceService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LoanRepository loanRepository;
    private final BorrowingQueries queries;
    private final LoanSettings settings;
    private final NotificationService notificationService;

    @Transactional
    public void run() {
        LocalDate today = LocalDate.now();
        int overdue = markOverdue(today);
        int reminded = remindDueSoon(today);
        int expired = expireUnclaimed(LocalDateTime.now());
        log.info("Loan maintenance: {} phiếu quá hạn, {} nhắc sắp đến hạn, {} yêu cầu hết hạn nhận sách",
                overdue, reminded, expired);
    }


    private int markOverdue(LocalDate today) {
        List<Loan> loans = loanRepository.findByStatusInAndDueDateBefore(
                Set.of(LoanStatus.BORROWED, LoanStatus.OVERDUE), today);
        int changed = 0;
        for (Loan loan : loans) {
            if (loan.getStatus() == LoanStatus.BORROWED) {
                loan.setStatus(LoanStatus.OVERDUE);
                changed++;
            }
            notificationService.notifyOnce(loan.getReader().getId(), NotificationType.OVERDUE,
                    "Sách đã quá hạn trả",
                    "Sách \"" + loan.getBook().getTitle() + "\" đã quá hạn trả từ ngày "
                            + loan.getDueDate().format(DATE) + ". Vui lòng trả sách sớm để tránh bị phạt thêm.",
                    loan.getId());
        }
        return changed;
    }


    private int remindDueSoon(LocalDate today) {
        List<Loan> loans = loanRepository.findByStatusAndDueDateBetween(
                LoanStatus.BORROWED, today, today.plusDays(settings.dueReminderDays()));
        int sent = 0;
        for (Loan loan : loans) {
            boolean created = notificationService.notifyOnce(loan.getReader().getId(), NotificationType.DUE_SOON,
                    "Sắp đến hạn trả sách",
                    "Sách \"" + loan.getBook().getTitle() + "\" đến hạn trả vào ngày "
                            + loan.getDueDate().format(DATE) + ". Bạn có thể gia hạn nếu cần.",
                    loan.getId());
            if (created) {
                sent++;
            }
        }
        return sent;
    }


    private int expireUnclaimed(LocalDateTime now) {
        List<Loan> loans = loanRepository.findByStatusAndPickupDeadlineBefore(LoanStatus.APPROVED, now);
        for (Loan loan : loans) {
            loan.setStatus(LoanStatus.EXPIRED);
            if (loan.getCopyId() != null) {
                queries.releaseHeldCopy(loan.getCopyId());
            }
            notificationService.notify(loan.getReader().getId(), NotificationType.SYSTEM,
                    "Yêu cầu mượn đã hết hạn nhận sách",
                    "Bạn không đến nhận \"" + loan.getBook().getTitle()
                            + "\" trước hạn nên yêu cầu mượn đã hết hiệu lực. Bạn có thể gửi yêu cầu mới.",
                    loan.getId());
        }
        return loans.size();
    }
}