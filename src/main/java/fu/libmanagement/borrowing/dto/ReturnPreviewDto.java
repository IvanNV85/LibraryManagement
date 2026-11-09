package fu.libmanagement.borrowing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO dữ liệu xem trước thông tin mượn khi quét Barcode hoặc tra cứu Mã phiếu mượn (UC18).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnPreviewDto {

    private Long loanId;
    private Long readerId;
    private String readerName;
    private String readerEmail;
    private String readerPhone;
    private String readerCardCode;

    private Long bookId;
    private String bookTitle;
    private String isbn;
    private String coverUrl;
    private Long copyId;
    private String barcode;
    private String shelfLocation;
    private BigDecimal bookPrice;

    private String borrowedAt;
    private String dueDate;
    private boolean overdue;
    private long overdueDays;
    private BigDecimal finePerDay;
    private BigDecimal estimatedOverdueFine;

    /**
     * Số người đang trong hàng chờ đặt trước đầu sách này (Reservation WAITING).
     */
    private long waitingReservations;
}

