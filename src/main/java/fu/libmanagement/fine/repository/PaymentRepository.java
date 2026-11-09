package fu.libmanagement.fine.repository;

import fu.libmanagement.fine.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByFineId(Long fineId);

    Optional<Payment> findFirstByFineIdOrderByPaidAtDesc(Long fineId);
}
