package fu.libmanagement.user.dto;

import fu.libmanagement.common.enums.AccountStatus;
import fu.libmanagement.common.enums.CardStatus;
import lombok.*;

import java.time.LocalDate;

/** Dữ liệu một độc giả trả về cho trang quản lý độc giả (UC6). */
@Getter
@RequiredArgsConstructor
public class ReaderResponse {

    private final Long cardId;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String cardCode;
    private final CardStatus status;
    private final LocalDate issuedDate;
    private final LocalDate expiryDate;
    private final boolean expired;
    private final AccountStatus accountStatus;
}
