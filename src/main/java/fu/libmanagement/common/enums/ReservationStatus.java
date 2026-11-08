package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum ReservationStatus {
    WAITING("Đang chờ sách", "bg-warning text-dark"),
    READY("Sách đã sẵn sàng", "bg-info text-dark"),
    FULFILLED("Đã mượn được", "bg-success"),
    CANCELLED("Đã hủy", "bg-secondary"),
    EXPIRED("Hết hạn giữ sách", "bg-secondary");

    private final String label;
    private final String badgeClass;

    public static final Set<ReservationStatus> ACTIVE = Set.of(WAITING, READY);
}