package fu.libmanagement.user.dto;

import fu.libmanagement.common.enums.AccountStatus;
import fu.libmanagement.common.enums.CardStatus;
import fu.libmanagement.user.entity.ReaderCard;
import fu.libmanagement.user.entity.User;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReaderResponse {

    private Long cardId;
    private String fullName;
    private String email;
    private String phone;
    private String cardCode;
    private CardStatus status;
    private LocalDate issuedDate;
    private LocalDate expiryDate;
    private boolean expired;
    private AccountStatus accountStatus;

    public static ReaderResponse from(ReaderCard card) {
        User user = card.getUser();
        boolean expired = card.getExpiryDate() != null && card.getExpiryDate().isBefore(LocalDate.now());

        return new ReaderResponse(
                card.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                card.getCardCode(),
                card.getStatus(),
                card.getIssuedDate(),
                card.getExpiryDate(),
                expired,
                user.getStatus());
    }
}
