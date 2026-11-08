package fu.libmanagement.engagement.repository;

import fu.libmanagement.engagement.entity.Favorite;
import fu.libmanagement.engagement.entity.FavoriteId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    boolean existsByIdReaderIdAndIdBookId(Long readerId, Long bookId);

    void deleteByIdReaderIdAndIdBookId(Long readerId, Long bookId);

    @Query("""
        select f from Favorite f
        where f.id.readerId = :readerId and f.book.visible = true
        order by f.createdAt desc
        """)
    Page<Favorite> findPageByReader(@Param("readerId") Long readerId, Pageable pageable);
}
