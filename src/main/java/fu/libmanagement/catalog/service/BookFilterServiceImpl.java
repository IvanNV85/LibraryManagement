package fu.libmanagement.catalog.service;

import fu.libmanagement.catalog.dto.BookSummary;
import fu.libmanagement.catalog.dto.SearchField;
import fu.libmanagement.catalog.entity.*;
import fu.libmanagement.catalog.repository.BookFilterRepository;
import fu.libmanagement.engagement.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class BookFilterServiceImpl implements BookFilterService {

    private static final int PAGE_SIZE = 10;

    private final BookFilterRepository repository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BookSummary> search(String keyword, SearchField field, long categoryId, int year,
                                    BookSort sort, int page) {
        String text = keyword.trim().toLowerCase();

        List<Book> books = repository.findByVisibleTrue();

        List<Book> filtered = new ArrayList<>();
        for (Book book : books) {
            if (categoryId != 0 && (book.getCategory() == null || book.getCategory().getId() != categoryId)) {
                continue;
            }
            if (year != 0 && (book.getPublishYear() == null || book.getPublishYear() != year)) {
                continue;
            }
            if (!matchKeyword(book, text, field)) {
                continue;
            }
            filtered.add(book);
        }

        sortBooks(filtered, sort);

        int pageNo = Math.max(page, 0);
        int from = Math.min(pageNo * PAGE_SIZE, filtered.size());
        int to = Math.min(from + PAGE_SIZE, filtered.size());

        List<BookSummary> summaries = new ArrayList<>();
        for (Book book : filtered.subList(from, to)) {
            summaries.add(toSummary(book));
        }
        return new PageImpl<>(summaries, PageRequest.of(pageNo, PAGE_SIZE), filtered.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getCategories() {
        return repository.findCategories();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getYears() {
        return repository.findYears();
    }

    private boolean matchKeyword(Book book, String text, SearchField field) {
        if (text.isEmpty()) {
            return true;
        }
        boolean all = field == SearchField.ALL;

        if ((all || field == SearchField.TITLE) && book.getTitle().toLowerCase().contains(text)) {
            return true;
        }
        if ((all || field == SearchField.ISBN) && book.getIsbn().toLowerCase().contains(text)) {
            return true;
        }
        if (all || field == SearchField.AUTHOR) {
            for (Author author : book.getAuthors()) {
                if (author.getName().toLowerCase().contains(text)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sortBooks(List<Book> books, BookSort sort) {
        books.sort(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER));

        if (sort == BookSort.NEWEST) {
            books.sort(Comparator.comparing(Book::getCreatedAt).reversed());
        }
        if (sort == BookSort.RATING) {
            Map<Long, Double> ratings = new HashMap<>();
            for (Book book : books) {
                Double avg = reviewRepository.averageRating(book.getId());
                ratings.put(book.getId(), avg == null ? 0.0 : avg);
            }
            books.sort((a, b) -> Double.compare(ratings.get(b.getId()), ratings.get(a.getId())));
        }
    }


    private BookSummary toSummary(Book book) {
        List<String> names = new ArrayList<>();
        for (Author author : book.getAuthors()) {
            names.add(author.getName());
        }
        Collections.sort(names);

        String category = book.getCategory() == null ? null : book.getCategory().getName();
        return new BookSummary(book.getId(), book.getTitle(), book.getIsbn(), book.getPublisher(),
                book.getPublishYear(), book.getCoverUrl(), category, String.join(", ", names));
    }
}
