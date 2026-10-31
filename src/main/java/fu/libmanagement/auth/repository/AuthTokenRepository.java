package fu.libmanagement.auth.repository;

import fu.libmanagement.auth.entity.AuthToken;
import fu.libmanagement.auth.entity.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    /** Lấy token kèm user trong 1 câu query (tránh query thêm khi dùng user). */
    @Query("select t from AuthToken t join fetch t.user where t.token = :token and t.type = :type")
    Optional<AuthToken> findWithUser(@Param("token") String token, @Param("type") TokenType type);

    /** Xóa các token cũ cùng loại của user bằng 1 câu delete. */
    @Modifying
    @Query("delete from AuthToken t where t.user.id = :userId and t.type = :type")
    void deleteTokensOfUser(@Param("userId") Long userId, @Param("type") TokenType type);
}
