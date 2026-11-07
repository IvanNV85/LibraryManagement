package fu.libmanagement.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LoanSource {
    ONLINE("Trực tuyến"),
    COUNTER("Tại quầy");

    private final String label;
}