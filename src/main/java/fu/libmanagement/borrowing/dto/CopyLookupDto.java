package fu.libmanagement.borrowing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thông tin tra cứu nhanh bản sao sách tại quầy thủ thư.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CopyLookupDto {
    private Long id;
    private String barcode;
    private Long bookId;
    private String bookTitle;
    private String isbn;
    private String coverUrl;
    private String shelfLocation;
    private String status;
    private boolean available;
    private String unavailableReason;
}

