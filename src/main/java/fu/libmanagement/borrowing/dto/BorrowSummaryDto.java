package fu.libmanagement.borrowing.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thống kê số lượng theo trạng thái phiếu mượn cho Thủ thư.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BorrowSummaryDto {
    private long pendingCount;
    private long approvedCount;
    private long borrowedCount;
    private long overdueCount;
}

