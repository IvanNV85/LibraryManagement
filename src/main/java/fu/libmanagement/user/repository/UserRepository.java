package fu.libmanagement.user.repository;

import fu.libmanagement.user.entity.User;
import fu.libmanagement.common.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Tra cứu email trên mọi vai trò, được dùng để kiểm tra trùng email khi thêm/sửa nhân viên. */
    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    @Query("""
        SELECT u FROM User u
        LEFT JOIN ReaderCard rc ON rc.user = u
        WHERE u.role = 'READER'
          AND (
            LOWER(u.email) = LOWER(:identifier)
            OR u.phone = :identifier
            OR rc.cardCode = :identifier
            OR CAST(u.id AS string) = :identifier
          )
        """)
    java.util.List<User> findReadersByIdentifier(@Param("identifier") String identifier);

    /**
     * Tìm tài khoản theo vai trò và họ tên/email, sắp xếp theo họ tên rồi ID, có phân trang.
     * StaffService truyền vai trò LIBRARIAN và từ khóa chữ thường dạng %từ khóa%.
     */
    @Query("select u from User u where u.role = :role and (lower(u.fullName) like :keyword or lower(u.email) like :keyword) order by u.fullName, u.id")
    Page<User> searchStaff(@Param("role") Role role, @Param("keyword") String keyword, Pageable pageable);
}
