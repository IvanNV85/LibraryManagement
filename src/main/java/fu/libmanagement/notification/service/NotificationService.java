package fu.libmanagement.notification.service;

import fu.libmanagement.borrowing.entity.Loan;
import fu.libmanagement.common.enums.NotificationType;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.notification.entity.Notification;
import fu.libmanagement.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification notify(Long userId, NotificationType type, String title, String content, Long refId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content == null ? null : limit(content, 500));
        n.setRefId(refId);
        return notificationRepository.save(n);
    }


    @Transactional
    public boolean notifyOnce(Long userId, NotificationType type, String title, String content, Long refId) {
        String text = content == null ? null : limit(content, 500);
        if (notificationRepository.existsByUserIdAndTypeAndRefIdAndContent(userId, type, refId, text)) {
            return false;
        }
        notify(userId, type, title, text, refId);
        return true;
    }


    @Transactional
    public void notifyLoanApproved(Loan loan) {
        String deadline = loan.getPickupDeadline() == null
                ? "" : " Vui lòng đến nhận sách trước " + loan.getPickupDeadline().format(DATE_TIME) + ".";
        notify(loan.getReader().getId(), NotificationType.LOAN_APPROVED, "Yêu cầu mượn đã được duyệt",
                "Yêu cầu mượn \"" + loan.getBook().getTitle() + "\" đã được duyệt." + deadline, loan.getId());
    }


    @Transactional
    public void notifyLoanRejected(Loan loan) {
        String reason = loan.getRejectReason() == null || loan.getRejectReason().isBlank()
                ? "" : " Lý do: " + loan.getRejectReason() + ".";
        notify(loan.getReader().getId(), NotificationType.LOAN_REJECTED, "Yêu cầu mượn bị từ chối",
                "Yêu cầu mượn \"" + loan.getBook().getTitle() + "\" đã bị từ chối." + reason, loan.getId());
    }


    @Transactional
    public void notifyFineCreated(Long readerId, Long loanId, String description) {
        notify(readerId, NotificationType.FINE_CREATED, "Bạn có khoản phạt mới", description, loanId);
    }

    // ------------------------------------------------------------- phía độc giả (UC15)


    @Transactional(readOnly = true)
    public Page<Notification> list(Long userId, int page) {
        return notificationRepository.findByUserId(userId, PageRequest.of(Math.max(page, 0), PAGE_SIZE,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndSeenFalse(userId);
    }


    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông báo"));
        n.setSeen(true);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllSeen(userId);
    }

    private String limit(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}