package fu.libmanagement.borrowing.service;

import fu.libmanagement.borrowing.dto.BorrowSummaryDto;
import fu.libmanagement.borrowing.dto.CopyLookupDto;
import fu.libmanagement.borrowing.dto.CounterLoanForm;
import fu.libmanagement.borrowing.dto.ReaderLookupDto;
import fu.libmanagement.borrowing.dto.ReturnBookForm;
import fu.libmanagement.borrowing.dto.ReturnPreviewDto;
import fu.libmanagement.borrowing.entity.Loan;
import fu.libmanagement.borrowing.repository.BorrowingQueries;
import fu.libmanagement.borrowing.repository.LoanRepository;
import fu.libmanagement.catalog.entity.Book;
import fu.libmanagement.common.enums.CardStatus;
import fu.libmanagement.common.enums.FineStatus;
import fu.libmanagement.common.enums.FineType;
import fu.libmanagement.common.enums.LoanSource;
import fu.libmanagement.common.enums.LoanStatus;
import fu.libmanagement.common.enums.NotificationType;
import fu.libmanagement.common.enums.ReservationStatus;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.common.util.ValidationUtil;
import fu.libmanagement.fine.entity.Fine;
import fu.libmanagement.fine.repository.FineRepository;
import fu.libmanagement.fine.service.FineService;
import fu.libmanagement.inventory.entity.BookCopy;
import fu.libmanagement.inventory.entity.CopyStatus;
import fu.libmanagement.inventory.repository.BookCopyRepository;
import fu.libmanagement.notification.entity.Notification;
import fu.libmanagement.notification.service.NotificationService;
import fu.libmanagement.reservation.repository.ReservationRepository;
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
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final FineRepository fineRepository;
    private final FineService fineService;
    private final ReservationRepository reservationRepository;

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

    // =========================================================================
    // UC18: GHI NHẬN TRẢ SÁCH (RETURN BOOK PROCESSING)
    // =========================================================================

    /**
     * Lấy danh sách các phiếu mượn đang lưu hành cần thu hồi (BORROWED / OVERDUE).
     */
    @Transactional(readOnly = true)
    public Page<Loan> getReturningLoans(String keyword, String statusFilter, int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
                Sort.by(Sort.Direction.ASC, "dueDate").and(Sort.by(Sort.Direction.DESC, "id")));

        String kw = ValidationUtil.trim(keyword).toLowerCase();
        String searchParam = kw.isEmpty() ? "" : "%" + kw + "%";

        List<LoanStatus> statuses;
        if ("BORROWED".equalsIgnoreCase(statusFilter)) {
            statuses = List.of(LoanStatus.BORROWED);
        } else if ("OVERDUE".equalsIgnoreCase(statusFilter)) {
            statuses = List.of(LoanStatus.OVERDUE);
        } else {
            statuses = List.of(LoanStatus.BORROWED, LoanStatus.OVERDUE);
        }

        return loanRepository.searchReturningLoans(searchParam, statuses, pageable);
    }

    /**
     * Lấy Map<copyId, BookCopy> cho danh sách các phiếu mượn trên trang hiện tại.
     */
    @Transactional(readOnly = true)
    public Map<Long, BookCopy> getReturnCopyMap(Page<Loan> page) {
        Set<Long> copyIds = page.getContent().stream()
                .map(Loan::getCopyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (copyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return bookCopyRepository.findAllById(copyIds).stream()
                .collect(Collectors.toMap(BookCopy::getId, Function.identity(), (a, b) -> a));
    }

    /**
     * Tra cứu xem trước thông tin phiếu mượn qua Barcode hoặc Mã phiếu mượn (#ID).
     */
    @Transactional(readOnly = true)
    public ReturnPreviewDto getReturnPreview(String identifier) {
        String cleanId = ValidationUtil.trim(identifier);
        if (cleanId.startsWith("#")) {
            cleanId = cleanId.substring(1).trim();
        }
        if (cleanId.isEmpty()) {
            throw new BusinessException("Vui lòng nhập Mã vạch bản sao hoặc Mã phiếu mượn.");
        }

        Loan activeLoan = null;
        BookCopy activeCopy = null;

        // 1. Thử tìm theo Barcode bản sao trước
        Optional<BookCopy> copyOpt = bookCopyRepository.findByBarcode(cleanId);
        if (copyOpt.isPresent()) {
            activeCopy = copyOpt.get();
            activeLoan = loanRepository.findFirstByCopyIdAndStatusIn(activeCopy.getId(),
                    List.of(LoanStatus.BORROWED, LoanStatus.OVERDUE)).orElse(null);
        }

        // 2. Nếu không tìm thấy qua Barcode, thử tra cứu theo Loan ID số
        if (activeLoan == null) {
            try {
                Long loanId = Long.parseLong(cleanId);
                Loan candidate = loanRepository.findWithDetailsById(loanId).orElse(null);
                if (candidate != null && (candidate.getStatus() == LoanStatus.BORROWED || candidate.getStatus() == LoanStatus.OVERDUE)) {
                    activeLoan = candidate;
                    if (activeLoan.getCopyId() != null) {
                        activeCopy = bookCopyRepository.findById(activeLoan.getCopyId()).orElse(null);
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (activeLoan == null) {
            throw new BusinessException("Không tìm thấy phiếu mượn nào đang lưu hành (Đang mượn / Quá hạn) khớp với mã: " + identifier);
        }

        LocalDate today = LocalDate.now();
        boolean overdue = activeLoan.getDueDate() != null && today.isAfter(activeLoan.getDueDate());
        long overdueDays = overdue ? ChronoUnit.DAYS.between(activeLoan.getDueDate(), today) : 0;
        BigDecimal finePerDay = settings.finePerDay();
        BigDecimal estimatedOverdueFine = finePerDay.multiply(BigDecimal.valueOf(overdueDays));

        ReaderCard card = readerCardRepository.findByUserId(activeLoan.getReader().getId()).orElse(null);
        long waitingReservations = reservationRepository.countByBookIdAndStatus(activeLoan.getBook().getId(), ReservationStatus.WAITING);

        return ReturnPreviewDto.builder()
                .loanId(activeLoan.getId())
                .readerId(activeLoan.getReader().getId())
                .readerName(activeLoan.getReader().getFullName())
                .readerEmail(activeLoan.getReader().getEmail())
                .readerPhone(activeLoan.getReader().getPhone())
                .readerCardCode(card != null ? card.getCardCode() : "—")
                .bookId(activeLoan.getBook().getId())
                .bookTitle(activeLoan.getBook().getTitle())
                .isbn(activeLoan.getBook().getIsbn())
                .coverUrl(activeLoan.getBook().getCoverUrl())
                .copyId(activeCopy != null ? activeCopy.getId() : activeLoan.getCopyId())
                .barcode(activeCopy != null ? activeCopy.getBarcode() : "—")
                .shelfLocation(activeCopy != null ? activeCopy.getShelfLocation() : "—")
                .bookPrice(activeCopy != null && activeCopy.getPrice() != null ? activeCopy.getPrice() : BigDecimal.ZERO)
                .borrowedAt(activeLoan.getBorrowedAt() != null ? activeLoan.getBorrowedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—")
                .dueDate(activeLoan.getDueDate() != null ? activeLoan.getDueDate().format(DATE_FMT) : "—")
                .overdue(overdue)
                .overdueDays(overdueDays)
                .finePerDay(finePerDay)
                .estimatedOverdueFine(estimatedOverdueFine)
                .waitingReservations(waitingReservations)
                .build();
    }

    /**
     * Thực hiện tiếp nhận và ghi nhận trả sách tại quầy.
     */
    @Transactional
    public String returnBook(ReturnBookForm form, Long staffUserId) {
        Loan loan = null;
        BookCopy copy = null;

        // 1. Xác định đúng phiếu mượn cần trả
        if (form.getLoanId() != null) {
            loan = loanRepository.findWithDetailsById(form.getLoanId())
                    .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu mượn với mã: #" + form.getLoanId()));
            if (loan.getCopyId() != null) {
                copy = bookCopyRepository.findById(loan.getCopyId()).orElse(null);
            }
        } else if (form.getBarcode() != null && !form.getBarcode().isBlank()) {
            String cleanBarcode = ValidationUtil.trim(form.getBarcode());
            if (cleanBarcode.startsWith("#")) cleanBarcode = cleanBarcode.substring(1).trim();

            Optional<BookCopy> copyOpt = bookCopyRepository.findByBarcode(cleanBarcode);
            if (copyOpt.isPresent()) {
                copy = copyOpt.get();
                loan = loanRepository.findFirstByCopyIdAndStatusIn(copy.getId(),
                        List.of(LoanStatus.BORROWED, LoanStatus.OVERDUE)).orElse(null);
            }
            if (loan == null) {
                try {
                    Long loanId = Long.parseLong(cleanBarcode);
                    Loan candidate = loanRepository.findWithDetailsById(loanId).orElse(null);
                    if (candidate != null && (candidate.getStatus() == LoanStatus.BORROWED || candidate.getStatus() == LoanStatus.OVERDUE)) {
                        loan = candidate;
                        if (loan.getCopyId() != null) {
                            copy = bookCopyRepository.findById(loan.getCopyId()).orElse(null);
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (loan == null) {
            throw new BusinessException("Vui lòng cung cấp Mã phiếu mượn hoặc Mã vạch bản sao sách hợp lệ.");
        }

        if (loan.getStatus() != LoanStatus.BORROWED && loan.getStatus() != LoanStatus.OVERDUE) {
            throw new BusinessException("Phiếu mượn #" + loan.getId() + " đang ở trạng thái '" 
                    + loan.getStatus().getLabel() + "', không thể thực hiện thao tác trả sách.");
        }

        User staff = userRepository.findById(staffUserId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin tài khoản thủ thư tiếp nhận."));

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        // 2. Cập nhật phiếu mượn sang RETURNED
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnedAt(now);
        loan.setReceivedBy(staffUserId);
        if (form.getNote() != null && !form.getNote().isBlank()) {
            loan.setReturnNote(ValidationUtil.trim(form.getNote()));
        }
        loanRepository.save(loan);

        // 3. Cập nhật tình trạng bản sao sách vật lý
        String condition = form.getCondition() != null ? form.getCondition().trim().toUpperCase() : "NORMAL";
        if (copy != null) {
            if ("DAMAGED".equals(condition)) {
                copy.setStatus(CopyStatus.DAMAGED);
            } else if ("LOST".equals(condition)) {
                copy.setStatus(CopyStatus.LOST);
            } else {
                copy.setStatus(CopyStatus.AVAILABLE);
            }
            bookCopyRepository.save(copy);
        }

        // 4. Xử lý các khoản phạt nếu có (Áp dụng cơ chế Upsert theo ràng buộc uq_fines_loan_type)
        List<Fine> createdFines = new ArrayList<>();
        NumberFormat nf = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

        // a) Phạt trễ hạn (nếu có)
        if (loan.getDueDate() != null && today.isAfter(loan.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(loan.getDueDate(), today);
            BigDecimal finePerDay = settings.finePerDay();
            BigDecimal overdueFineAmount = finePerDay.multiply(BigDecimal.valueOf(overdueDays));

            Fine overdueFine = fineRepository.findByLoanIdAndFineType(loan.getId(), FineType.OVERDUE).orElse(null);
            if (overdueFine == null) {
                overdueFine = new Fine();
                overdueFine.setReader(loan.getReader());
                overdueFine.setLoan(loan);
                overdueFine.setFineType(FineType.OVERDUE);
                overdueFine.setStatus(FineStatus.UNPAID);
                overdueFine.setCreatedAt(now);
            }

            overdueFine.setOverdueDays((int) overdueDays);
            overdueFine.setAmount(overdueFineAmount);
            overdueFine.setDescription("Trả sách trễ hạn " + overdueDays + " ngày (mức phạt " 
                    + nf.format(finePerDay) + " đ/ngày). Phiếu mượn #" + loan.getId());
            overdueFine.setCreatedBy(staff);

            fineRepository.save(overdueFine);
            createdFines.add(overdueFine);
        }

        // b) Phạt hư hỏng hoặc báo mất sách (nếu có)
        if ("DAMAGED".equals(condition)) {
            BigDecimal damageAmount = (form.getCustomFine() != null && form.getCustomFine().signum() > 0)
                    ? form.getCustomFine()
                    : BigDecimal.valueOf(50000); // Mặc định 50.000đ

            Fine damageFine = fineRepository.findByLoanIdAndFineType(loan.getId(), FineType.DAMAGED).orElse(null);
            if (damageFine == null) {
                damageFine = new Fine();
                damageFine.setReader(loan.getReader());
                damageFine.setLoan(loan);
                damageFine.setFineType(FineType.DAMAGED);
                damageFine.setStatus(FineStatus.UNPAID);
                damageFine.setCreatedAt(now);
            }

            damageFine.setAmount(damageAmount);
            damageFine.setDescription("Bồi thường sách bị hư hỏng khi trả. " 
                    + (form.getNote() != null && !form.getNote().isBlank() ? "(" + form.getNote() + ")" : ""));
            damageFine.setCreatedBy(staff);

            fineRepository.save(damageFine);
            createdFines.add(damageFine);
        } else if ("LOST".equals(condition)) {
            BigDecimal lostAmount = (form.getCustomFine() != null && form.getCustomFine().signum() > 0)
                    ? form.getCustomFine()
                    : (copy != null && copy.getPrice() != null && copy.getPrice().signum() > 0 
                            ? copy.getPrice() 
                            : BigDecimal.valueOf(100000)); // Mặc định theo giá bản sao hoặc 100.000đ

            Fine lostFine = fineRepository.findByLoanIdAndFineType(loan.getId(), FineType.LOST).orElse(null);
            if (lostFine == null) {
                lostFine = new Fine();
                lostFine.setReader(loan.getReader());
                lostFine.setLoan(loan);
                lostFine.setFineType(FineType.LOST);
                lostFine.setStatus(FineStatus.UNPAID);
                lostFine.setCreatedAt(now);
            }

            lostFine.setAmount(lostAmount);
            lostFine.setDescription("Bồi thường làm mất sách (Giá trị: " + nf.format(lostAmount) + " đ). Phiếu mượn #" + loan.getId());
            lostFine.setCreatedBy(staff);

            fineRepository.save(lostFine);
            createdFines.add(lostFine);
        }

        // 5. Thu tiền phạt nhanh tại quầy nếu thủ thư chọn
        boolean paidAtCounter = false;
        if (form.isPayFineNow() && !createdFines.isEmpty()) {
            for (Fine f : createdFines) {
                if (f.getStatus() != FineStatus.PAID) {
                    fineService.payFineAtCounter(f.getId(), staffUserId);
                }
            }
            paidAtCounter = true;
        }

        // 6. Gửi thông báo đến độc giả
        for (Fine f : createdFines) {
            if (!paidAtCounter && f.getStatus() == FineStatus.UNPAID) {
                notificationService.notifyFineCreated(loan.getReader().getId(), loan.getId(), f.getDescription());
            }
        }
        notificationService.notify(loan.getReader().getId(), NotificationType.SYSTEM,
                "Ghi nhận trả sách thành công",
                "Bạn đã hoàn tất trả cuốn sách \"" + loan.getBook().getTitle() + "\". Cảm ơn bạn!",
                loan.getId());

        // 7. Chuẩn bị thông báo kết quả cho thủ thư
        StringBuilder msg = new StringBuilder();
        msg.append("Đã ghi nhận trả sách thành công cho phiếu #").append(loan.getId())
           .append(" (").append(loan.getBook().getTitle()).append(").");

        if (!createdFines.isEmpty()) {
            BigDecimal totalFine = createdFines.stream().map(Fine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (paidAtCounter) {
                msg.append(" Đã thu tiền phạt tại quầy: ").append(nf.format(totalFine)).append(" đ.");
            } else {
                msg.append(" Đã phát sinh khoản phạt ").append(nf.format(totalFine))
                   .append(" đ (Chưa thu, ghi nợ trên tài khoản độc giả).");
            }
        }

        long waiting = (copy != null && copy.getStatus() == CopyStatus.AVAILABLE)
                ? reservationRepository.countByBookIdAndStatus(loan.getBook().getId(), ReservationStatus.WAITING)
                : 0;
        if (waiting > 0) {
            msg.append(" 🔔 Lưu ý: Đầu sách này đang có ").append(waiting).append(" độc giả trong hàng chờ đặt trước!");
        }

        log.info("Thủ thư #{} đã ghi nhận trả sách cho phiếu #{}. Kết quả: {}", staffUserId, loan.getId(), msg);
        return msg.toString();
    }
}

