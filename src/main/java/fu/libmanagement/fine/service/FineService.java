package fu.libmanagement.fine.service;

import fu.libmanagement.common.enums.FineStatus;
import fu.libmanagement.common.enums.PaymentMethod;
import fu.libmanagement.common.enums.PaymentStatus;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.common.util.ValidationUtil;
import fu.libmanagement.fine.dto.FineSummaryDto;
import fu.libmanagement.fine.entity.Fine;
import fu.libmanagement.fine.entity.Payment;
import fu.libmanagement.fine.repository.FineRepository;
import fu.libmanagement.fine.repository.PaymentRepository;
import fu.libmanagement.user.entity.User;
import fu.libmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Xử lý nghiệp vụ tiền phạt và thu tiền phạt tại quầy (UC19).
 */
@Service
@RequiredArgsConstructor
public class FineService {

    public static final int PAGE_SIZE = 10;

    private final FineRepository fineRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách khoản phạt cho thủ thư, có tìm kiếm, lọc trạng thái và phân trang.
     */
    @Transactional(readOnly = true)
    public Page<Fine> getFines(String keyword, FineStatus status, int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        String kw = ValidationUtil.trim(keyword).toLowerCase();
        String searchParam = kw.isEmpty() ? "" : "%" + kw + "%";
        return fineRepository.searchFines(searchParam, status, pageable);
    }

    /**
     * Lấy tóm tắt thống kê các chỉ số phạt cho Dashboard và bộ lọc.
     */
    @Transactional(readOnly = true)
    public FineSummaryDto getSummary() {
        BigDecimal unpaidSum = fineRepository.sumAmountByStatus(FineStatus.UNPAID);
        long unpaidCount = fineRepository.countByStatus(FineStatus.UNPAID);
        BigDecimal paidSum = fineRepository.sumAmountByStatus(FineStatus.PAID);
        long paidCount = fineRepository.countByStatus(FineStatus.PAID);

        return new FineSummaryDto(
                unpaidSum != null ? unpaidSum : BigDecimal.ZERO,
                unpaidCount,
                paidSum != null ? paidSum : BigDecimal.ZERO,
                paidCount
        );
    }

    /**
     * Chi tiết khoản phạt theo ID.
     */
    @Transactional(readOnly = true)
    public Fine getFineById(Long id) {
        return fineRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy khoản phạt với mã: " + id));
    }

    /**
     * Xác nhận thu tiền phạt trực tiếp tại quầy từ độc giả.
     * Cập nhật Fine sang PAID và tạo giao dịch Payment.
     */
    @Transactional
    public void payFineAtCounter(Long fineId, Long staffUserId) {
        Fine fine = getFineById(fineId);

        if (fine.getStatus() == FineStatus.PAID) {
            throw new BusinessException("Khoản phạt này đã được thanh toán trước đó.");
        }

        User staff = userRepository.findById(staffUserId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin thủ thư tiếp nhận thu phạt."));

        LocalDateTime now = LocalDateTime.now();

        // 1. Cập nhật trạng thái Fine
        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(now);
        fineRepository.save(fine);

        // 2. Ghi nhận giao dịch thanh toán
        Payment payment = new Payment();
        payment.setFine(fine);
        payment.setAmount(fine.getAmount());
        payment.setMethod(PaymentMethod.COUNTER);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionCode("PAY-" + System.currentTimeMillis() + "-" + fine.getId());
        payment.setReceivedBy(staff);
        payment.setCreatedAt(now);
        payment.setPaidAt(now);
        paymentRepository.save(payment);
    }
}

