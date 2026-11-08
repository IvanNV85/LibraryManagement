package fu.libmanagement.user.repository;

import fu.libmanagement.user.entity.ReaderCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Truy vấn riêng cho UC6 (tách khỏi ReaderCardRepository để không sửa code của module khác). */
public interface ReaderManagementRepository extends JpaRepository<ReaderCard, Long> {

    /**
     * Tìm độc giả theo họ tên, email hoặc mã thẻ, lọc theo trạng thái thẻ.
     * status là ALL hoặc tên một CardStatus; kw đã ở dạng chữ thường kèm dấu % hai đầu.
     * join fetch nạp luôn tài khoản để khỏi query lẻ từng dòng.
     */
    @Query(value = """
            select rc from ReaderCard rc join fetch rc.user u
            where (:status = 'ALL' or cast(rc.status as string) = :status)
              and (   lower(u.fullName) like :kw
                   or lower(u.email) like :kw
                   or lower(coalesce(rc.cardCode, '')) like :kw)
            order by u.createdAt desc, rc.id desc
            """,
            countQuery = """
            select count(rc) from ReaderCard rc join rc.user u
            where (:status = 'ALL' or cast(rc.status as string) = :status)
              and (   lower(u.fullName) like :kw
                   or lower(u.email) like :kw
                   or lower(coalesce(rc.cardCode, '')) like :kw)
            """)
    Page<ReaderCard> search(@Param("status") String status, @Param("kw") String kw, Pageable pageable);
}
