package fu.libmanagement.report.repository;

import java.sql.Date;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Truy vấn trực tiếp các phiếu mượn hiện có, không cần bảng báo cáo. */
@Repository
@RequiredArgsConstructor
public class LoanReportRepository {
    private final JdbcTemplate jdbc;

    public long countOnLoan() {
        return jdbc.queryForObject("SELECT COUNT_BIG(*) FROM loans WHERE status IN ('BORROWED', 'OVERDUE')", Long.class);
    }

    public long countOverdue(LocalDate today) {
        return jdbc.queryForObject("SELECT COUNT_BIG(*) FROM loans WHERE status = 'OVERDUE' "
                + "OR (status = 'BORROWED' AND due_date < ?)", Long.class, Date.valueOf(today));
    }
}
