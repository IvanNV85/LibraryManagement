package fu.libmanagement.borrowing.dto;

import fu.libmanagement.common.enums.LoanStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BorrowBookInfo {

    private final Long id;
    private final String title;
    private final String isbn;
    private final String coverUrl;
    private final String authorNames;
    private final String categoryName;

    private final long availableCopies;

    private final LoanStatus activeLoanStatus;

    private final boolean reserved;
}