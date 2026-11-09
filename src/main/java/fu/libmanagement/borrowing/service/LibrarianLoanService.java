package fu.libmanagement.borrowing.service;

import fu.libmanagement.borrowing.dto.BorrowSummaryDto;
import fu.libmanagement.borrowing.dto.CopyLookupDto;
import fu.libmanagement.borrowing.dto.CounterLoanForm;
import fu.libmanagement.borrowing.dto.ReaderLookupDto;
import fu.libmanagement.borrowing.entity.Loan;
import fu.libmanagement.borrowing.repository.BorrowingQueries;
import fu.libmanagement.borrowing.repository.LoanRepository;
import fu.libmanagement.catalog.entity.Book;
import fu.libmanagement.common.enums.CardStatus;
import fu.libmanagement.common.enums.LoanSource;
import fu.libmanagement.common.enums.LoanStatus;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.common.util.ValidationUtil;
import fu.libmanagement.inventory.entity.BookCopy;
import fu.libmanagement.inventory.entity.CopyStatus;
import fu.libmanagement.inventory.repository.BookCopyRepository;
import fu.libmanagement.notification.service.NotificationService;
import fu.libmanagement.user.entity.ReaderCard;
import fu.libmanagement.user.entity.User;
import fu.libmanagement.user.repository.ReaderCardRepository;
import fu.libmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Xử lý nghiệp vụ Mượn sách dành cho Thủ thư (UC16, UC17).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LibrarianLoanService {

    public static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LoanRepository loanRepository;
    private final BookCopyRepository bookCopyRepository;
    private final UserRepository userRepository;
    private final ReaderCardRepository readerCardRepository;
    private final BorrowRuleService borrowRuleService;
    private final BorrowingQueries queries;
    private final LoanSettings settings;
    private final NotificationService notificationService;

    // =========================================================================
    // UC16: DUYỆT YÊU CẦU MƯỢN ONLINE
    // =========================================================================

    /**
     * Lấy danh sách yêu cầu mượn online với tìm kiếm, lọc trạng thái và phân trang.
     */
    @Transactional(readOnly = true)
    public Page<Loan> getBorrowRequests(String keyword, LoanStatus status, int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "requestedAt", "id"));
        String kw = ValidationUtil.trim(keyword).toLowerCase();
        String searchParam = kw.isEmpty() ? "" : "%" + kw + "%";
        return loanRepository.searchLibrarianLoans(searchParam, LoanSource.ONLINE, status, pageable);
    }

    /**
     * Thống kê các chỉ số phiếu mượn cho Thủ thư.
     */
    @Transactional(readOnly = true)
    public BorrowSummaryDto getSummary() {
        long pending = loanRepository.countBySourceAndStatus(LoanSource.ONLINE, LoanStatus.PENDING);
        long approved = loanRepository.countByStatus(LoanStatus.APPROVED);
        long borrowed = loanRepository.countByStatus(LoanStatus.BORROWED);
        long overdue = loanRepository.countByStatus(LoanStatus.OVERDUE);

        return new BorrowSummaryDto(pending, approved, borrowed, overdue);
    }

    /**
     * Lấy danh sách các bản sao còn sẵn sàng (AVAILABLE) của một đầu sách.
     */
    @Transactional(readOnly = true)
    public List<BookCopy> getAvailableCopies(Long bookId) {
        return bookCopyRepository.findByBookIdAndStatus(bookId, CopyStatus.AVAILABLE);
    }

    /**
     * Lấy chi tiết phiếu theo ID (nạp đầy đủ reader và book).
     */
    @Transactional(readOnly = true)
    public Loan getLoanById(Long loanId) {
        return loanRepository.findWithDetailsById(loanId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu mượn với mã: #" + loanId));
    }

    /**
     * Thủ thư duyệt yêu cầu mượn online: gán bản sao vật lý, chuyển HELD và hẹn ngày nhận sách.
     */
    @Transactional
    public void approveRequest(Long loanId, Long copyId, Long staffId) {
        Loan loan = getLoanById(loanId);

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new BusinessException("Chỉ có thể duyệt yêu cầu đang ở trạng thái 'Chờ duyệt' (PENDING).");
        }

        // Kiểm tra lại điều kiện của độc giả trước khi duyệt
        borrowRuleService.requireReaderEligible(loan.getReader().getId());

        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy bản sao sách đã chọn."));

        if (!copy.getBook().getId().equals(loan.getBook().getId())) {
            throw new BusinessException("Bản sao được chọn không thuộc đúng đầu sách của yêu cầu này.");
        }

        if (copy.getStatus() != CopyStatus.AVAILABLE) {
            throw new BusinessException("Bản sao này hiện không khả dụng (Trạng thái: " + copy.getStatus() + ").");
        }

        LocalDateTime now = LocalDateTime.now();
        int holdDays = settings.pickupHoldDays();
        LocalDateTime pickupDeadline = now.plusDays(holdDays);

        // 1. Cập nhật bản sao sang HELD (giữ sách)
        copy.setStatus(CopyStatus.HELD);
        bookCopyRepository.save(copy);

        // 2. Cập nhật phiếu sang APPROVED
        loan.setCopyId(copy.getId());
        loan.setStatus(LoanStatus.APPROVED);
        loan.setReviewedBy(staffId);
        loan.setReviewedAt(now);
        loan.setPickupDeadline(pickupDeadline);
        loanRepository.save(loan);

        // 3. Gửi thông báo đến độc giả
        notificationService.notifyLoanApproved(loan);
        log.info("Thủ thư #{} đã duyệt yêu cầu mượn #{}, gán bản sao barcode: {}", staffId, loanId, copy.getBarcode());
    }

    /**
     * Thủ thư từ chối yêu cầu mượn online kèm lý do.
     */
    @Transactional
    public void rejectRequest(Long loanId, String reason, Long staffId) {
        Loan loan = getLoanById(loanId);

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new BusinessException("Chỉ có thể từ chối yêu cầu đang ở trạng thái 'Chờ duyệt' (PENDING).");
        }

        String rejectReason = ValidationUtil.trim(reason);
        if (rejectReason.isEmpty()) {
            rejectReason = "Thư viện tạm thời chưa thể đáp ứng yêu cầu mượn này.";
        }

        loan.setStatus(LoanStatus.REJECTED);
        loan.setRejectReason(rejectReason);
        loan.setReviewedBy(staffId);
        loan.setReviewedAt(LocalDateTime.now());
        loanRepository.save(loan);

        // Gửi thông báo từ chối tới độc giả
        notificationService.notifyLoanRejected(loan);
        log.info("Thủ thư #{} đã từ chối yêu cầu mượn #{}. Lý do: {}", staffId, loanId, rejectReason);
    }

    /**
     * Xác nhận giao sách khi độc giả đến nhận (APPROVED -> BORROWED).
     */
    @Transactional
    public void issueApprovedLoan(Long loanId, Long staffId) {
        Loan loan = getLoanById(loanId);

        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new BusinessException("Phiếu này không ở trạng thái 'Đã duyệt chờ nhận sách' (APPROVED).");
        }

        if (loan.getPickupDeadline() != null && loan.getPickupDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Yêu cầu này đã quá hạn nhận sách (" 
                    + loan.getPickupDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ").");
        }

        if (loan.getCopyId() == null) {
            throw new BusinessException("Yêu cầu chưa được gán bản sao vật lý.");
        }

        BookCopy copy = bookCopyRepository.findById(loan.getCopyId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy bản sao sách đã gán cho phiếu này."));

        LocalDateTime now = LocalDateTime.now();
        int loanDays = settings.loanDays();
        LocalDate dueDate = LocalDate.now().plusDays(loanDays);

        // Cập nhật bản sao sang BORROWED
        copy.setStatus(CopyStatus.BORROWED);
        bookCopyRepository.save(copy);

        // Cập nhật phiếu mượn sang BORROWED
        loan.setStatus(LoanStatus.BORROWED);
        loan.setIssuedBy(staffId);
        loan.setBorrowedAt(now);
        loan.setDueDate(dueDate);
        loanRepository.save(loan);

        log.info("Thủ thư #{} đã giao sách cho phiếu #{}. Hạn trả: {}", staffId, loanId, dueDate);
    }

    // =========================================================================
    // UC17: LẬP PHIẾU MƯỢN TRỰC TIẾP TẠI QUẦY
    // =========================================================================

    /**
     * Tra cứu nhanh độc giả theo Email, Mã thẻ hoặc SĐT để kiểm tra điều kiện mượn.
     */
    @Transactional(readOnly = true)
    public ReaderLookupDto lookupReader(String identifier) {
        String cleanId = ValidationUtil.trim(identifier);
        if (cleanId.isEmpty()) {
            throw new BusinessException("Vui lòng nhập Email, Mã thẻ hoặc Số điện thoại để tìm kiếm.");
        }

        List<User> readers = userRepository.findReadersByIdentifier(cleanId);
        if (readers.isEmpty()) {
            throw new BusinessException("Không tìm thấy độc giả nào với thông tin: " + cleanId);
        }

        User reader = readers.get(0);
        ReaderCard card = readerCardRepository.findByUserId(reader.getId()).orElse(null);

        long activeLoans = loanRepository.countByReaderIdAndStatusIn(reader.getId(), LoanStatus.ACTIVE);
        int maxBooks = settings.maxBooksPerReader();
        BigDecimal unpaid = queries.sumUnpaidFines(reader.getId());
        boolean hasOverdue = loanRepository.existsByReaderIdAndStatus(reader.getId(), LoanStatus.OVERDUE);

        boolean eligible = true;
        String ineligibleReason = null;

        if (card == null || card.getStatus() != CardStatus.ACTIVE) {
            eligible = false;
            ineligibleReason = "Thẻ thư viện chưa được kích hoạt hoặc đang bị khóa.";
        } else if (card.getExpiryDate() != null && card.getExpiryDate().isBefore(LocalDate.now())) {
            eligible = false;
            ineligibleReason = "Thẻ thư viện đã hết hạn vào ngày " + card.getExpiryDate().format(DATE_FMT) + ".";
        } else if (unpaid.signum() > 0) {
            eligible = false;
            ineligibleReason = "Độc giả còn nợ khoản phạt chưa thanh toán (" + unpaid + " đ).";
        } else if (hasOverdue) {
            eligible = false;
            ineligibleReason = "Độc giả đang có sách quá hạn chưa trả.";
        } else if (activeLoans >= maxBooks) {
            eligible = false;
            ineligibleReason = "Độc giả đã mượn tối đa số sách cho phép (" + maxBooks + " cuốn).";
        }

        return ReaderLookupDto.builder()
                .id(reader.getId())
                .fullName(reader.getFullName())
                .email(reader.getEmail())
                .phone(reader.getPhone() != null ? reader.getPhone() : "Chưa cập nhật")
                .cardNumber(card != null && card.getCardCode() != null ? card.getCardCode() : "Chưa cấp")
                .cardStatus(card != null && card.getStatus() != null ? card.getStatus().name() : "NONE")
                .expiryDate(card != null && card.getExpiryDate() != null ? card.getExpiryDate().format(DATE_FMT) : "Chưa có")
                .activeLoansCount(activeLoans)
                .maxBooks(maxBooks)
                .unpaidFines(unpaid)
                .eligible(eligible)
                .ineligibleReason(ineligibleReason)
                .build();
    }

    /**
     * Tra cứu nhanh bản sao sách theo barcode.
     */
    @Transactional(readOnly = true)
    public CopyLookupDto lookupCopy(String barcode) {
        String cleanBarcode = ValidationUtil.trim(barcode);
        if (cleanBarcode.isEmpty()) {
            throw new BusinessException("Vui lòng nhập hoặc quét mã vạch bản sao.");
        }

        BookCopy copy = bookCopyRepository.findByBarcode(cleanBarcode)
                .orElseThrow(() -> new BusinessException("Không tìm thấy bản sao nào với mã vạch: " + cleanBarcode));

        Book book = copy.getBook();
        boolean available = copy.getStatus() == CopyStatus.AVAILABLE;
        String unavailableReason = available ? null : "Bản sao này đang ở trạng thái: " + copy.getStatus();

        return CopyLookupDto.builder()
                .id(copy.getId())
                .barcode(copy.getBarcode())
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .isbn(book.getIsbn())
                .coverUrl(book.getCoverUrl())
                .shelfLocation(copy.getShelfLocation() != null ? copy.getShelfLocation() : "Chưa xếp giá")
                .status(copy.getStatus().name())
                .available(available)
                .unavailableReason(unavailableReason)
                .build();
    }

    /**
     * Lập phiếu mượn trực tiếp tại quầy: kiểm tra toàn diện và tạo thẳng ở trạng thái BORROWED.
     */
    @Transactional
    public Loan createCounterLoan(CounterLoanForm form, Long staffId) {
        if (form.getReaderIdentifier() == null || form.getReaderIdentifier().trim().isEmpty()) {
            throw new BusinessException("Vui lòng nhập Email, Mã thẻ hoặc SĐT của độc giả.");
        }
        if (form.getBarcode() == null || form.getBarcode().trim().isEmpty()) {
            throw new BusinessException("Vui lòng nhập hoặc quét mã vạch bản sao sách.");
        }

        // 1. Tìm và kiểm tra độc giả
        List<User> readers = userRepository.findReadersByIdentifier(form.getReaderIdentifier().trim());
        if (readers.isEmpty()) {
            throw new BusinessException("Không tìm thấy độc giả với thông tin: " + form.getReaderIdentifier());
        }
        User reader = readers.get(0);

        // Kiểm tra điều kiện mượn của độc giả
        borrowRuleService.requireReaderEligible(reader.getId());

        // 2. Tìm và kiểm tra bản sao
        BookCopy copy = bookCopyRepository.findByBarcode(form.getBarcode().trim())
                .orElseThrow(() -> new BusinessException("Không tìm thấy bản sao với mã vạch: " + form.getBarcode()));

        if (copy.getStatus() != CopyStatus.AVAILABLE) {
            throw new BusinessException("Bản sao này hiện không khả dụng để mượn (Trạng thái: " + copy.getStatus() + ").");
        }

        // 3. Kiểm tra độc giả đã có yêu cầu/phiếu active với cuốn sách này chưa
        borrowRuleService.requireNoActiveLoanForBook(reader.getId(), copy.getBook().getId());

        LocalDateTime now = LocalDateTime.now();
        int loanDays = settings.loanDays();
        LocalDate dueDate = LocalDate.now().plusDays(loanDays);

        // 4. Cập nhật bản sao sang BORROWED
        copy.setStatus(CopyStatus.BORROWED);
        bookCopyRepository.save(copy);

        // 5. Tạo bản ghi Loan mới
        Loan loan = new Loan();
        loan.setReader(reader);
        loan.setBook(copy.getBook());
        loan.setCopyId(copy.getId());
        loan.setSource(LoanSource.COUNTER);
        loan.setStatus(LoanStatus.BORROWED);
        loan.setRequestedAt(now);
        loan.setBorrowedAt(now);
        loan.setIssuedBy(staffId);
        loan.setDueDate(dueDate);
        loan.setReturnNote(ValidationUtil.trim(form.getNote()).isEmpty() ? null : ValidationUtil.trim(form.getNote()));

        Loan saved = loanRepository.save(loan);
        log.info("Lập phiếu mượn tại quầy thành công! Mã phiếu: #{}, Độc giả: {}, Barcode: {}, Hạn trả: {}",
                saved.getId(), reader.getEmail(), copy.getBarcode(), dueDate);

        return saved;
    }

    // =========================================================================
    // QUẢN LÝ TỔNG HỢP PHIẾU MƯỢN THƯ VIỆN
    // =========================================================================

    /**
     * Tra cứu danh sách toàn bộ phiếu mượn (cả Online và Counter) cho Thủ thư.
     */
    @Transactional(readOnly = true)
    public Page<Loan> getAllLoans(String keyword, LoanSource source, LoanStatus status, int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "borrowedAt", "requestedAt", "id"));
        String kw = ValidationUtil.trim(keyword).toLowerCase();
        String searchParam = kw.isEmpty() ? "" : "%" + kw + "%";
        return loanRepository.searchLibrarianLoans(searchParam, source, status, pageable);
    }
}

