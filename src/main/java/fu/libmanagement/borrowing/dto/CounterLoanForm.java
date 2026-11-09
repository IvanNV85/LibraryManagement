package fu.libmanagement.borrowing.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Form dữ liệu lập phiếu mượn trực tiếp tại quầy (UC17).
 */
@Getter
@Setter
public class CounterLoanForm {

    /** Email, Mã thẻ thư viện, hoặc Số điện thoại của độc giả */
    private String readerIdentifier;

    /** Mã vạch của bản sao sách vật lý mang đến quầy */
    private String barcode;

    /** Ghi chú bổ sung (nếu có) */
    private String note;
}

