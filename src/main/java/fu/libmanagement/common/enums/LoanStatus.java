package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum LoanStatus {
    PENDING("Chờ duyệt", "bg-warning text-dark", 1),
    APPROVED("Đã duyệt, chờ nhận sách", "bg-info text-dark", 2),
    REJECTED("Bị từ chối", "bg-danger", 0),
    CANCELLED("Đã hủy", "bg-secondary", 0),
    EXPIRED("Hết hạn nhận sách", "bg-secondary", 0),
    BORROWED("Đang mượn", "bg-primary", 3),
    OVERDUE("Quá hạn", "bg-danger", 3),
    RETURNED("Đã trả", "bg-success", 4);


    private final String label;

    private final String badgeClass;

    private final int step;

    public static final Set<LoanStatus> ACTIVE = Set.of(PENDING, APPROVED, BORROWED, OVERDUE);
}