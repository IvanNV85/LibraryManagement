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

    public static final int MAX_REVIEWS = 10;

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

        String authors = book.getAuthors().stream()
                .map(Author::getName).sorted().collect(Collectors.joining(", "));
        String category = book.getCategory() == null ? null : book.getCategory().getName();

        int totalCopies = stockRepository.countCopies(bookId);
        int availableCopies = stockRepository.countAvailable(bookId);
        List<String> shelves = stockRepository.findShelves(bookId);

        Double avg = reviewRepository.averageRating(bookId);
        Double averageRating = avg == null ? null : Math.round(avg * 10) / 10.0;

        boolean favorite = readerId != null && favoriteService.isFavorite(readerId, bookId);

        return BookDetailResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .isbn(book.getIsbn())
                .publisher(book.getPublisher())
                .publishYear(book.getPublishYear())
                .description(book.getDescription())
                .coverUrl(book.getCoverUrl())
                .categoryName(category)
                .authorNames(authors)
                .totalCopies(totalCopies)
                .availableCopies(availableCopies)
                .shelfLocations(shelves.isEmpty() ? null : String.join(", ", shelves))
                .averageRating(averageRating)
                .reviewCount(reviewRepository.countByBookId(bookId))
                .favorite(favorite)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getLatestReviews(Long bookId) {
        return reviewRepository.findLatest(bookId, PageRequest.of(0, MAX_REVIEWS)).stream()
                .map(r -> new ReviewResponse(r.getReader().getFullName(), r.getRating(),
                        r.getComment(), r.getCreatedAt()))
                .toList();
    }
}
