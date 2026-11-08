package fu.libmanagement.report.service;

import fu.libmanagement.inventory.entity.CopyStatus;
import fu.libmanagement.inventory.repository.BookCopyRepository;
import fu.libmanagement.report.repository.LoanReportRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final LoanReportRepository loans;
    private final BookCopyRepository copies;

    @Transactional(readOnly = true)
    public Statistics statistics() {
        return new Statistics(loans.countOnLoan(), loans.countOverdue(LocalDate.now()),
                copies.countByStatus(CopyStatus.AVAILABLE));
    }

    public record Statistics(long onLoan, long overdue, long available) { }
}
