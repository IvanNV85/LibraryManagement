package fu.libmanagement.engagement.service;

import fu.libmanagement.catalog.dto.BookSummary;
import fu.libmanagement.catalog.entity.Author;
import fu.libmanagement.catalog.entity.Book;
import fu.libmanagement.catalog.repository.BookRepository;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.engagement.entity.Favorite;
import fu.libmanagement.engagement.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    public static final int PAGE_SIZE = 10;

    private final FavoriteRepository favoriteRepository;
    private final BookRepository bookRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BookSummary> list(Long readerId, int page) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
        return favoriteRepository.findPageByReader(readerId, pageable)
                .map(f -> toSummary(f.getBook()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(Long readerId, Long bookId) {
        return favoriteRepository.existsByIdReaderIdAndIdBookId(readerId, bookId);
    }

    @Override
    @Transactional
    public void add(Long readerId, Long bookId) {
        Book book = bookRepository.findById(bookId)
                .filter(Book::isVisible)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đầu sách"));

        if (favoriteRepository.existsByIdReaderIdAndIdBookId(readerId, bookId)) {
            throw new BusinessException("Sách đã có trong danh sách yêu thích");
        }
        favoriteRepository.save(new Favorite(readerId, book));
    }

    @Override
    @Transactional
    public void remove(Long readerId, Long bookId) {
        favoriteRepository.deleteByIdReaderIdAndIdBookId(readerId, bookId);
    }

    private BookSummary toSummary(Book book) {
        String authors = book.getAuthors().stream()
                .map(Author::getName).sorted().collect(Collectors.joining(", "));
        String category = book.getCategory() == null ? null : book.getCategory().getName();
        return new BookSummary(book.getId(), book.getTitle(), book.getIsbn(), book.getPublisher(),
                book.getPublishYear(), book.getCoverUrl(), category, authors);
    }
}
