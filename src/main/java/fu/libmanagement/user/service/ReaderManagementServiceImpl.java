package fu.libmanagement.user.service;

import fu.libmanagement.common.enums.CardStatus;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.user.dto.ReaderResponse;
import fu.libmanagement.user.entity.ReaderCard;
import fu.libmanagement.user.entity.User;
import fu.libmanagement.user.repository.ReaderManagementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReaderManagementServiceImpl implements ReaderManagementService {

    public static final int PAGE_SIZE = 10;

    private static final int CARD_VALID_MONTHS = 12;

    private final ReaderManagementRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<ReaderResponse> search(String keyword, String status, int page) {
        String kw = "%" + (keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT)) + "%";
        PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
        // map trong transaction để đọc được thông tin tài khoản
        return repository.search(normalizeStatus(status), kw, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public void approve(Long cardId) {
        ReaderCard card = getCard(cardId);
        if (card.getStatus() != CardStatus.PENDING) {
            throw new BusinessException("Chỉ duyệt được thẻ đang chờ duyệt");
        }
        LocalDate today = LocalDate.now();
        card.setCardCode(String.format("LIB%06d", card.getId()));
        card.setIssuedDate(today);
        card.setExpiryDate(today.plusMonths(CARD_VALID_MONTHS));
        card.setStatus(CardStatus.ACTIVE);
    }

    @Override
    @Transactional
    public void lock(Long cardId) {
        ReaderCard card = getCard(cardId);
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new BusinessException("Chỉ khóa được thẻ đang hoạt động");
        }
        card.setStatus(CardStatus.LOCKED);
    }

    @Override
    @Transactional
    public void unlock(Long cardId) {
        ReaderCard card = getCard(cardId);
        if (card.getStatus() != CardStatus.LOCKED) {
            throw new BusinessException("Thẻ này không bị khóa");
        }
        if (card.getCardCode() == null) {
            throw new BusinessException("Thẻ chưa được duyệt, hãy duyệt thẻ trước");
        }
        card.setStatus(CardStatus.ACTIVE);
    }

    /** Tính từ ngày hết hạn cũ, hoặc từ hôm nay nếu thẻ đã hết hạn. */
    @Override
    @Transactional
    public void extend(Long cardId) {
        ReaderCard card = getCard(cardId);
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new BusinessException("Chỉ gia hạn được thẻ đang hoạt động");
        }
        LocalDate today = LocalDate.now();
        LocalDate base = card.getExpiryDate() != null && card.getExpiryDate().isAfter(today)
                ? card.getExpiryDate() : today;
        card.setExpiryDate(base.plusMonths(CARD_VALID_MONTHS));
    }

    private ReaderCard getCard(Long cardId) {
        return repository.findById(cardId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thẻ thư viện"));
    }

    /** Chỉ nhận ALL hoặc tên một CardStatus; giá trị lạ thì coi như ALL. */
    private String normalizeStatus(String status) {
        try {
            return CardStatus.valueOf(status).name();
        } catch (IllegalArgumentException | NullPointerException e) {
            return "ALL";
        }
    }

    private ReaderResponse toResponse(ReaderCard card) {
        User user = card.getUser();
        boolean expired = card.getExpiryDate() != null && card.getExpiryDate().isBefore(LocalDate.now());
        return new ReaderResponse(card.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                card.getCardCode(), card.getStatus(), card.getIssuedDate(), card.getExpiryDate(),
                expired, user.getStatus());
    }
}
