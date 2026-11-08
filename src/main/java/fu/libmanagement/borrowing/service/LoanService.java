package fu.libmanagement.borrowing.service;

import fu.libmanagement.borrowing.dto.BorrowBookInfo;
import fu.libmanagement.borrowing.dto.LoanView;
import fu.libmanagement.borrowing.entity.Loan;
import fu.libmanagement.borrowing.repository.BorrowingQueries;
import fu.libmanagement.borrowing.repository.BorrowingQueries.FineSummary;
import fu.libmanagement.borrowing.repository.LoanRepository;
import fu.libmanagement.catalog.entity.Author;
import fu.libmanagement.catalog.entity.Book;
import fu.libmanagement.catalog.repository.BookRepository;
import fu.libmanagement.common.enums.LoanSource;
import fu.libmanagement.common.enums.LoanStatus;
import fu.libmanagement.common.enums.ReservationStatus;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.reservation.entity.Reservation;
import fu.libmanagement.reservation.repository.ReservationRepository;
import fu.libmanagement.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class LoanService {

    public static final int PAGE_SIZE = 10;

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final ReservationRepository reservationRepository;
    private final BorrowRuleService ruleService;
    private final BorrowingQueries queries;
    private final LoanSettings settings;
    private final UserService userService;


    @Transactional(readOnly = true)
    public BorrowBookInfo getBorrowInfo(Long readerId, Long bookId) {
        Book book = getVisibleBook(bookId);

        LoanStatus activeStatus = loanRepository
                .findFirstByReaderIdAndBookIdAndStatusIn(readerId, bookId, LoanStatus.ACTIVE)
                .map(Loan::getStatus)
                .orElse(null);
        boolean reserved = reservationRepository.existsByReaderIdAndBookIdAndStatusIn(
                readerId, bookId, ReservationStatus.ACTIVE);

        String authors = book.getAuthors().stream().map(Author::getName).sorted().collect(Collectors.joining(", "));
        String category = book.getCategory() == null ? null : book.getCategory().getName();
        return new BorrowBookInfo(book.getId(), book.getTitle(), book.getIsbn(), book.getCoverUrl(),
                authors, category, queries.countAvailableCopies(bookId), activeStatus, reserved);
    }


    @Transactional
    public Loan requestLoan(Long readerId, Long bookId) {
        Book book = getVisibleBook(bookId);
        ruleService.requireCanBorrow(readerId, bookId);

        Loan loan = new Loan();
        loan.setReader(userService.getUser(readerId));
        loan.setBook(book);
        loan.setSource(LoanSource.ONLINE);
        loan.setStatus(LoanStatus.PENDING);
        loan.setRequestedAt(LocalDateTime.now());

        try {
            loanRepository.saveAndFlush(loan);
        } catch (DataIntegrityViolationException e) {
            // hai lần bấm cùng lúc: filtered index uq_loans_active_reader_book chặn dòng thứ hai
            throw new BusinessException("Bạn đang mượn hoặc đã gửi yêu cầu mượn cuốn sách này");
        }

        // Nếu độc giả từng đặt trước cuốn này thì đặt trước đó đã có kết quả
        for (Reservation r : reservationRepository.findByReaderIdAndBookIdAndStatusIn(
                readerId, bookId, ReservationStatus.ACTIVE)) {
            r.setStatus(ReservationStatus.FULFILLED);
        }
        return loan;
    }


    @Transactional
    public void cancelRequest(Long readerId, Long loanId) {
        Loan loan = getOwnLoan(readerId, loanId);
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new BusinessException("Chỉ hủy được yêu cầu đang chờ duyệt. Nếu đã được duyệt, hãy liên hệ thủ thư");
        }
        loan.setStatus(LoanStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public Page<LoanView> history(Long readerId, LoanStatus status, int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
                Sort.by(Sort.Order.desc("requestedAt"), Sort.Order.desc("id")));
        Page<Loan> loans = status == null
                ? loanRepository.findByReaderId(readerId, pageable)
                : loanRepository.findByReaderIdAndStatus(readerId, status, pageable);

        Map<Long, FineSummary> fines = queries.findFinesByLoan(readerId,
                loans.getContent().stream().map(Loan::getId).toList());
        LocalDate today = LocalDate.now();
        int maxRenewals = settings.maxRenewals();
        BigDecimal finePerDay = settings.finePerDay();

        return loans.map(l -> toView(l, fines.get(l.getId()), today, maxRenewals, finePerDay));
    }

    private LoanView toView(Loan l, FineSummary fine, LocalDate today, int maxRenewals, BigDecimal finePerDay) {
        boolean inHand = l.getStatus() == LoanStatus.BORROWED || l.getStatus() == LoanStatus.OVERDUE;
        boolean pastDue = inHand && l.getDueDate() != null && l.getDueDate().isBefore(today);
        boolean canRenew = l.getStatus() == LoanStatus.BORROWED && !pastDue && l.getRenewCount() < maxRenewals;

        String renewNote = null;
        if (inHand && !canRenew) {
            renewNote = pastDue || l.getStatus() == LoanStatus.OVERDUE
                    ? "Đã quá hạn, không thể gia hạn"
                    : "Đã dùng hết lượt gia hạn";
        }

        BigDecimal fineAmount = null;
        boolean fineUnpaid = false;
        boolean fineEstimated = false;
        if (fine != null) {
            fineAmount = fine.total();
            fineUnpaid = fine.unpaid();
        } else if (pastDue) {
            long lateDays = ChronoUnit.DAYS.between(l.getDueDate(), today);
            fineAmount = finePerDay.multiply(BigDecimal.valueOf(lateDays));
            fineUnpaid = true;
            fineEstimated = true;
        }

        Book book = l.getBook();
        return new LoanView(l.getId(), book.getId(), book.getTitle(), book.getCoverUrl(), l.getStatus(),
                l.getRequestedAt(), l.getPickupDeadline(), l.getDueDate(), l.getReturnedAt(),
                l.getRenewCount(), maxRenewals, l.getRejectReason(),
                fineAmount, fineUnpaid, fineEstimated,
                canRenew, l.getStatus() == LoanStatus.PENDING, renewNote);
    }

    // ------------------------------------------------------------------ UC14

    @Transactional
    public Loan renew(Long readerId, Long loanId) {
        Loan loan = getOwnLoan(readerId, loanId);

        if (loan.getStatus() != LoanStatus.BORROWED) {
            throw new BusinessException(loan.getStatus() == LoanStatus.OVERDUE
                    ? "Phiếu đã quá hạn, không thể gia hạn. Vui lòng trả sách tại quầy"
                    : "Chỉ gia hạn được sách đang mượn");
        }
        if (loan.getDueDate() == null || loan.getDueDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Phiếu đã quá hạn, không thể gia hạn. Vui lòng trả sách tại quầy");
        }
        int max = settings.maxRenewals();
        if (loan.getRenewCount() >= max) {
            throw new BusinessException("Phiếu này đã gia hạn tối đa " + max + " lần");
        }

        loan.setDueDate(loan.getDueDate().plusDays(settings.renewDays()));
        loan.setRenewCount(loan.getRenewCount() + 1);
        return loan;
    }

    // ------------------------------------------------------------------ dùng chung

    private Loan getOwnLoan(Long readerId, Long loanId) {
        // tìm theo cả reader_id: độc giả không thao tác được phiếu của người khác
        return loanRepository.findByIdAndReaderId(loanId, readerId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu mượn"));
    }

    private Book getVisibleBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sách"));
        if (!book.isVisible()) {
            throw new BusinessException("Sách này hiện không còn trong danh mục");
        }
        return book;
    }
}