package fu.libmanagement.auth.service;

import fu.libmanagement.auth.dto.RegisterForm;
import fu.libmanagement.auth.dto.SessionUser;
import fu.libmanagement.common.enums.AccountStatus;
import fu.libmanagement.common.enums.CardStatus;
import fu.libmanagement.common.enums.Role;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.common.util.ValidationUtil;
import fu.libmanagement.user.entity.ReaderCard;
import fu.libmanagement.user.entity.User;
import fu.libmanagement.user.repository.ReaderCardRepository;
import fu.libmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC1 Đăng ký tài khoản độc giả và UC2 Đăng nhập. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String WRONG_CREDENTIALS = "Email hoặc mật khẩu không đúng";

    private final UserRepository userRepository;
    private final ReaderCardRepository readerCardRepository;
    private final PasswordHelper passwordHelper;

    // ===================== UC1: Đăng ký =====================

    /**
     * Tạo tài khoản độc giả (role READER, status ACTIVE) và một thẻ thư viện ở trạng thái PENDING.
     * Thủ thư sẽ duyệt thẻ sau (UC6), lúc đó thẻ mới có mã thẻ và ngày hết hạn.
     */
    @Transactional
    public void register(RegisterForm form) {
        String fullName = ValidationUtil.trim(form.getFullName());
        String email = ValidationUtil.normalizeEmail(form.getEmail());

        ValidationUtil.checkFullName(fullName);
        if (!ValidationUtil.isValidEmail(email)) {
            throw new BusinessException("Email không hợp lệ");
        }
        passwordHelper.validateNewPassword(form.getPassword(), form.getConfirmPassword());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email này đã được đăng ký");
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordHelper.hash(form.getPassword()));
        user.setRole(Role.READER);
        user.setStatus(AccountStatus.ACTIVE);
        try {
            // saveAndFlush để lỗi trùng email (hai người đăng ký cùng lúc) xảy ra ngay tại đây
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Email này đã được đăng ký");
        }

        ReaderCard card = new ReaderCard();
        card.setUser(user);
        card.setStatus(CardStatus.PENDING);
        readerCardRepository.save(card);
    }

    // ===================== UC2: Đăng nhập =====================

    /** Kiểm tra email và mật khẩu, trả về thông tin để lưu vào session. Sai thì ném BusinessException. */
    @Transactional(readOnly = true)
    public SessionUser login(String email, String password) {
        User user = userRepository.findByEmail(ValidationUtil.normalizeEmail(email))
                .orElseThrow(() -> new BusinessException(WRONG_CREDENTIALS));

        if (!passwordHelper.matches(password, user.getPasswordHash())) {
            throw new BusinessException(WRONG_CREDENTIALS);
        }
        if (user.getStatus() == AccountStatus.LOCKED) {
            throw new BusinessException("Tài khoản đã bị khóa, vui lòng liên hệ thư viện");
        }
        return SessionUser.of(user);
    }
}
