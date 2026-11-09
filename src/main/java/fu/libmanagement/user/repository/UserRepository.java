package fu.libmanagement.user.repository;

import fu.libmanagement.common.enums.Role;
import fu.libmanagement.user.entity.ReaderCard;
import fu.libmanagement.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    @Query("""
        SELECT u FROM User u
        LEFT JOIN ReaderCard rc ON rc.user = u
        WHERE u.role = fu.libmanagement.common.enums.Role.READER
          AND (
            LOWER(u.email) = LOWER(:identifier)
            OR u.phone = :identifier
            OR rc.cardCode = :identifier
            OR CAST(u.id AS string) = :identifier
          )
        """)
    List<User> findReadersByIdentifier(@Param("identifier") String identifier);

    @Query("select u from User u where u.role = :role and (lower(u.fullName) like :keyword or lower(u.email) like :keyword) order by u.fullName, u.id")
    Page<User> searchStaff(@Param("role") Role role, @Param("keyword") String keyword, Pageable pageable);
}
