package fu.libmanagement.auth.service;

import fu.libmanagement.auth.dto.RegisterForm;
import fu.libmanagement.auth.dto.SessionUser;
import fu.libmanagement.auth.entity.AuthToken;
import fu.libmanagement.auth.entity.TokenType;
import fu.libmanagement.common.enums.AccountStatus;
import fu.libmanagement.common.enums.CardStatus;
import fu.libmanagement.common.enums.Role;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.common.util.MailService;
import fu.libmanagement.common.util.ValidationUtil;
import fu.libmanagement.user.entity.ReaderCard;
import fu.libmanagement.user.entity.User;
import fu.libmanagement.user.repository.ReaderCardRepository;
import fu.libmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/** UC1  UC2 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String WRONG_CREDENTIALS = "Email hoặc mật khẩu không đúng";

    private final UserRepository userRepository;
    private final ReaderCardRepository readerCardRepository;
    private final TokenService tokenService;
    private final PasswordHelper passwordHelper;
    private final MailService mailService;

    @Value("${app.auth.max-failed-logins}")
    private int maxFailedLogins;

    @Value("${app.auth.lock-minutes}")
    private int lockMinutes;

    @Value("${app.auth.verify-token-hours}")
    private int verifyTokenHours;

    // ===================== UC1: Đăng ký =====================

    /**
     * Tạo tài khoản độc giả (chưa xác thực email) và thẻ thư viện ở trạng thái chờ duyệt,
     * sau đó gửi email xác thực.
     */
    @Transactional
    public void register(RegisterForm form) {
        String fullName = ValidationUtil.trim(form.getFullName());
        String email = ValidationUtil.normalizeEmail(form.getEmail());
        String phone = ValidationUtil.trim(form.getPhone());

        ValidationUtil.checkNameAndPhone(fullName, phone);
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
        user.setPhone(phone);
        user.setPasswordHash(passwordHelper.hash(form.getPassword()));
        user.setRole(Role.READER);
        user.setStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(false);
        userRepository.save(user);

        ReaderCard card = new ReaderCard();
        card.setUser(user);
        card.setStatus(CardStatus.PENDING);
        readerCardRepository.save(card);

        sendVerificationEmail(user);
    }

    /** Xác thực email bằng token trong liên kết. Trả về true nếu thành công. */
    @Transactional
    public boolean verifyEmail(String token) {
        Optional<AuthToken> found = tokenService.findValid(token, TokenType.EMAIL_VERIFY);
        if (found.isEmpty()) {
            return false;
        }
        AuthToken authToken = found.get();
        authToken.getUser().setEmailVerified(true); // entity đang được quản lý nên tự lưu khi commit
        tokenService.delete(authToken);
        return true;
    }

    /** Gửi lại email xác thực. Không báo email có tồn tại hay không để tránh dò tài khoản. */
    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(ValidationUtil.normalizeEmail(email))
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::sendVerificationEmail);
    }

    private void sendVerificationEmail(User user) {
        String token = tokenService.createToken(user, TokenType.EMAIL_VERIFY, Duration.ofHours(verifyTokenHours));
        mailService.sendLink(user.getEmail(), "Xác thực email tài khoản thư viện",
                "Xin chào " + user.getFullName() + ",\n\n"
                        + "Vui lòng bấm vào liên kết dưới đây để xác thực email (có hiệu lực "
                        + verifyTokenHours + " giờ):",
                "/verify-email", token,
                "Sau khi xác thực, tài khoản của bạn sẽ chờ thủ thư duyệt thẻ thư viện.");
    }

    // ===================== UC2: Đăng nhập =====================

    /**
     * Kiểm tra email và mật khẩu, trả về thông tin để lưu vào session.
     * Dùng noRollbackFor để số lần đăng nhập sai vẫn được lưu dù method ném BusinessException.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public SessionUser login(String email, String password) {
        User user = userRepository.findByEmail(ValidationUtil.normalizeEmail(email))
                .orElseThrow(() -> new BusinessException(WRONG_CREDENTIALS));

        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new BusinessException("Tài khoản tạm khóa do đăng nhập sai nhiều lần, thử lại sau "
                    + user.getLockedUntil().format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        if (!passwordHelper.matches(password, user.getPasswordHash())) {
            int failed = user.getFailedLoginCount() + 1;
            if (failed >= maxFailedLogins) {
                user.setLockedUntil(now.plusMinutes(lockMinutes));
                user.setFailedLoginCount(0);
                throw new BusinessException("Bạn đã nhập sai " + maxFailedLogins
                        + " lần, tài khoản bị khóa " + lockMinutes + " phút");
            }
            user.setFailedLoginCount(failed);
            throw new BusinessException(WRONG_CREDENTIALS);
        }

        if (user.getStatus() == AccountStatus.LOCKED) {
            throw new BusinessException("Tài khoản đã bị khóa, vui lòng liên hệ thư viện");
        }
        if (!user.isEmailVerified()) {
            throw new BusinessException("Email chưa được xác thực, vui lòng kiểm tra hộp thư");
        }

        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        return SessionUser.of(user);
    }
}
