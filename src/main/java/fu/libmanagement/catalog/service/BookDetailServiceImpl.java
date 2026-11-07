package fu.libmanagement.catalog.service;

import fu.libmanagement.catalog.dto.BookDetailResponse;
import fu.libmanagement.catalog.entity.Author;
import fu.libmanagement.catalog.entity.Book;
import fu.libmanagement.catalog.repository.BookRepository;
import fu.libmanagement.catalog.repository.BookStockRepository;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.engagement.dto.ReviewResponse;
import fu.libmanagement.engagement.repository.ReviewRepository;
import fu.libmanagement.engagement.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookDetailServiceImpl implements BookDetailService {

    private static final int MAX_REVIEWS = 10;

    private final BookRepository bookRepository;
    private final BookStockRepository stockRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteService favoriteService;

    @Override
    @Transactional(readOnly = true)
    public BookDetailResponse getDetail(Long bookId, Long readerId) {
        Book book = bookRepository.findById(bookId)
                .filter(Book::isVisible)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đầu sách"));


        String authors = book.getAuthors().stream().map(Author::getName).sorted().collect(Collectors.joining(", "));
        String categoryName = book.getCategory() == null ? null : book.getCategory().getName();

        List<String> shelves = stockRepository.findShelves(bookId);
        String shelfLocations = shelves.isEmpty() ? null : String.join(", ", shelves);

        Double avg = reviewRepository.averageRating(bookId);       // null nếu chưa có đánh giá
        Double averageRating = avg == null ? null : Math.round(avg * 10) / 10.0;

        boolean favorite = readerId != null && favoriteService.isFavorite(readerId, bookId);

        return new BookDetailResponse(
                book.getId(),
                book.getTitle(),
                book.getIsbn(),
                book.getPublisher(),
                book.getPublishYear(),
                book.getDescription(),
                book.getCoverUrl(),
                categoryName,
                authors,
                stockRepository.countCopies(bookId),
                stockRepository.countAvailable(bookId),
                shelfLocations,
                averageRating,
                reviewRepository.countByBookId(bookId),
                favorite);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getLatestReviews(Long bookId) {
        return reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId, PageRequest.of(0, MAX_REVIEWS))
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }
}
