package fu.libmanagement.fine.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FineSummaryDto {

    private BigDecimal totalUnpaidAmount = BigDecimal.ZERO;
    private long unpaidCount = 0;
    private BigDecimal totalPaidAmount = BigDecimal.ZERO;
    private long paidCount = 0;
}

