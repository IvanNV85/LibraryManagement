package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum LoanStatus {
    PENDING("Chờ duyệt", "bg-warning text-dark"),
    APPROVED("Đã duyệt, chờ nhận sách", "bg-info text-dark"),
    REJECTED("Bị từ chối", "bg-danger"),
    CANCELLED("Đã hủy", "bg-secondary"),
    EXPIRED("Hết hạn nhận sách", "bg-secondary"),
    BORROWED("Đang mượn", "bg-primary"),
    OVERDUE("Quá hạn", "bg-danger"),
    RETURNED("Đã trả", "bg-success");

    private final String label;

    private final String badgeClass;

    public static final Set<LoanStatus> ACTIVE = Set.of(PENDING, APPROVED, BORROWED, OVERDUE);
}