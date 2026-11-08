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

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select u from User u where u.role = :role and (lower(u.fullName) like :keyword or lower(u.email) like :keyword) order by u.fullName, u.id")
    Page<User> searchStaff(@Param("role") Role role, @Param("keyword") String keyword, Pageable pageable);
}
