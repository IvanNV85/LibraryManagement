package fu.libmanagement.user.repository;

import fu.libmanagement.user.entity.ReaderCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReaderCardRepository extends JpaRepository<ReaderCard, Long> {

    Optional<ReaderCard> findByUserId(Long userId);
}
