package fu.libmanagement.engagement.service;

import fu.libmanagement.catalog.dto.BookSummary;
import org.springframework.data.domain.Page;

//UC9: Quản lý danh sách yêu thích
public interface FavoriteService {

    Page<BookSummary> list(Long readerId, int page);

    boolean isFavorite(Long readerId, Long bookId);

    void add(Long readerId, Long bookId);

    void remove(Long readerId, Long bookId);
}
