package fu.libmanagement.borrowing.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Form tiếp nhận ghi nhận trả sách tại quầy (UC18).
 */
@Getter
@Setter
public class ReturnBookForm {

    /**
     * ID phiếu mượn (khi chọn từ danh sách hoặc sau khi tra cứu).
     */
    private Long loanId;

    /**
     * Mã vạch bản sao sách (khi quét bằng máy barcode).
     */
    private String barcode;

    /**
     * Tình trạng sách khi trả lại:
     * NORMAL: Nguyên vẹn (bình thường, tái sử dụng được)
     * DAMAGED: Bị hư hỏng (rách, bẩn, mất trang)
     * LOST: Báo mất sách
     */
    private String condition = "NORMAL";

    /**
     * Tiền phạt phụ thu đối với sách hư hỏng hoặc mất (nếu thủ thư nhập thủ công).
     */
    private BigDecimal customFine;

    /**
     * Ghi chú tình trạng sách khi trả.
     */
    private String note;

    /**
     * Tùy chọn: Độc giả nộp phạt tiền mặt ngay tại quầy.
     * Nếu true, hệ thống sẽ tự động gạch nợ sang PAID và lưu giao dịch thanh toán.
     */
    private boolean payFineNow;
}

