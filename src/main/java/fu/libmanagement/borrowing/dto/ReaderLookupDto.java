package fu.libmanagement.borrowing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Thông tin tra cứu nhanh độc giả tại quầy thủ thư.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReaderLookupDto {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String cardNumber;
    private String cardStatus;
    private String expiryDate;
    private long activeLoansCount;
    private int maxBooks;
    private BigDecimal unpaidFines;
    private boolean eligible;
    private String ineligibleReason;
}

