package fu.libmanagement.catalog.repository;

import fu.libmanagement.catalog.entity.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {

    List<Publisher> findAllByOrderByNameAsc();
}

