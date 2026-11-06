package fu.libmanagement.catalog.service;

import fu.libmanagement.catalog.dto.BookSummary;
import fu.libmanagement.catalog.dto.SearchField;
import fu.libmanagement.catalog.entity.Author;
import fu.libmanagement.catalog.entity.Book;
import fu.libmanagement.catalog.entity.BookSort;
import fu.libmanagement.catalog.repository.BookRepository;
import fu.libmanagement.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** UC5 Tìm kiếm sách (theo tên, tác giả, ISBN), gợi ý từ khóa, phân trang. */
@Service
@RequiredArgsConstructor
public class BookService {

    public static final int PAGE_SIZE = 10;
    private static final int MAX_SUGGESTIONS = 8;

    private final BookRepository bookRepository;

    /** Tìm sách theo từ khóa; từ khóa rỗng thì liệt kê tất cả. Kết quả sắp xếp theo tên sách. */
    @Transactional(readOnly = true)
    public Page<BookSummary> search(String keyword, SearchField field, int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("title"));
        String text = normalize(keyword);

        Page<Book> books = text.isEmpty()
                ? bookRepository.findByVisibleTrue(pageable)
                : bookRepository.search(field.name(), "%" + text + "%", pageable);

        // map trong transaction để tác giả và thể loại được nạp theo lô (xem @BatchSize)
        return books.map(this::toSummary);
    }

    /** Gợi ý tối đa 8 tên sách chứa từ khóa (cần ít nhất 2 ký tự). */
    @Transactional(readOnly = true)
    public List<String> suggest(String keyword) {
        String text = normalize(keyword);
        if (text.length() < 2) {
            return List.of();
        }
        return bookRepository.findTitleSuggestions("%" + text + "%", PageRequest.of(0, MAX_SUGGESTIONS));
    }

    /** Sách mới nhất cho trang chủ. */
    @Transactional(readOnly = true)
    public List<BookSummary> getNewestBooks() {
        return bookRepository.findTop5ByVisibleTrueOrderByCreatedAtDescIdDesc().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<BookSummary> searchAndFilter(String keyword, String field, Long categoryId, Integer publishYear, BookSort sortType, int page) {
        if (sortType == null) {
            sortType = BookSort.TITLE;
        }

        Sort sort = switch (sortType) {
            case NEWEST -> Sort.by(Sort.Direction.DESC, "publishYear", "id");
            case RATING -> Sort.by(Sort.Direction.DESC, "id");
            case TITLE -> Sort.by("title");
        };

        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, sort);
        String kw = normalize(keyword);
        if (field == null || field.isEmpty()) {
            field = "ALL";
        }

        Page<Book> books = bookRepository.searchAndFilter(field, "%" + kw + "%", categoryId, publishYear, pageable);
        return books.map(this::toSummary);
    }

    private String normalize(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    }

    private BookSummary toSummary(Book book) {
        String authors = book.getAuthors().stream()
                .map(Author::getName)
                .sorted()
                .collect(Collectors.joining(", "));
        String category = book.getCategory() == null ? null : book.getCategory().getName();
        return new BookSummary(book.getId(), book.getTitle(), book.getIsbn(), book.getPublisher(),
                book.getPublishYear(), book.getCoverUrl(), category, authors);
    }
}
