package fu.libmanagement.auth.service;

import fu.libmanagement.auth.entity.AuthToken;
import fu.libmanagement.auth.entity.TokenType;
import fu.libmanagement.auth.repository.AuthTokenRepository;
import fu.libmanagement.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** Tạo, tìm và xóa token gửi qua email (xác thực email, đặt lại mật khẩu). */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final AuthTokenRepository tokenRepository;

    /** Tạo token mới; token cũ cùng loại của user bị xóa nên mỗi user chỉ có 1 token hợp lệ mỗi loại. */
    @Transactional
    public String createToken(User user, TokenType type, Duration validFor) {
        tokenRepository.deleteTokensOfUser(user.getId(), type);

        AuthToken authToken = new AuthToken();
        authToken.setToken(UUID.randomUUID().toString());
        authToken.setType(type);
        authToken.setUser(user);
        authToken.setExpiresAt(LocalDateTime.now().plus(validFor));
        tokenRepository.save(authToken);
        return authToken.getToken();
    }

    /** Tìm token đúng loại và còn hạn; sai hoặc hết hạn thì trả về rỗng. */
    @Transactional(readOnly = true)
    public Optional<AuthToken> findValid(String token, TokenType type) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return tokenRepository.findWithUser(token, type)
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    /** Dùng xong thì xóa để token không dùng lại được. */
    @Transactional
    public void delete(AuthToken token) {
        tokenRepository.delete(token);
    }
}
