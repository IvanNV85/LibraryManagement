package fu.libmanagement.user.service;

import fu.libmanagement.common.enums.CardStatus;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.user.entity.ReaderCard;
import fu.libmanagement.user.repository.ReaderCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Tra cứu và kiểm tra thẻ thư viện, dùng chung cho các use case sau:
 * UC11 (gửi yêu cầu mượn) và UC17 (lập phiếu tại quầy) gọi {@link #requireValidCard}
 * để kiểm tra điều kiện "thẻ còn hạn"; UC6 dùng {@link #getCard} để duyệt thẻ.
 */
@Service
@RequiredArgsConstructor
public class ReaderCardService {

    private final ReaderCardRepository readerCardRepository;

    /** Thẻ của độc giả (null nếu tài khoản không có thẻ, ví dụ nhân viên). */
    @Transactional(readOnly = true)
    public ReaderCard getCard(Long userId) {
        return readerCardRepository.findByUserId(userId).orElse(null);
    }

    /** true nếu thẻ đang ACTIVE và chưa hết hạn. */
    @Transactional(readOnly = true)
    public boolean isCardValid(Long userId) {
        try {
            requireValidCard(userId);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    /** Kiểm tra thẻ dùng được để mượn sách. Không dùng được thì ném BusinessException nêu rõ lý do. */
    @Transactional(readOnly = true)
    public void requireValidCard(Long userId) {
        ReaderCard card = getCard(userId);
        if (card == null) {
            throw new BusinessException("Tài khoản chưa có thẻ thư viện");
        }
        if (card.getStatus() == CardStatus.PENDING) {
            throw new BusinessException("Thẻ thư viện đang chờ thủ thư duyệt");
        }
        if (card.getStatus() == CardStatus.LOCKED) {
            throw new BusinessException("Thẻ thư viện đang bị khóa");
        }
        if (card.getExpiryDate() != null && card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Thẻ thư viện đã hết hạn");
        }
    }

}
