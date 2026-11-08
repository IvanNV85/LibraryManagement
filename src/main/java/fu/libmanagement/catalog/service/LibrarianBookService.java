package fu.libmanagement.catalog.service;

import fu.libmanagement.catalog.dto.BookForm;
import fu.libmanagement.catalog.entity.Author;
import fu.libmanagement.catalog.entity.Book;
import fu.libmanagement.catalog.entity.Category;
import fu.libmanagement.catalog.entity.Publisher;
import fu.libmanagement.catalog.repository.AuthorRepository;
import fu.libmanagement.catalog.repository.BookRepository;
import fu.libmanagement.catalog.repository.CategoryRepository;
import fu.libmanagement.catalog.repository.PublisherRepository;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.common.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Xử lý nghiệp vụ quản lý đầu sách dành cho Thủ thư (UC20).
 */
@Service
@RequiredArgsConstructor
public class LibrarianBookService {

    public static final int PAGE_SIZE = 10;

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;

    /**
     * Lấy danh sách đầu sách cho thủ thư, có phân trang và tìm kiếm.
     */
    @Transactional(readOnly = true)
    public Page<Book> getBooks(String keyword, int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"));
        String kw = ValidationUtil.trim(keyword).toLowerCase();
        String searchParam = kw.isEmpty() ? "" : "%" + kw + "%";
        return bookRepository.searchForLibrarian(searchParam, pageable);
    }

    /**
     * Danh sách tất cả thể loại để đổ vào form dropdown.
     */
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    /**
     * Danh sách tất cả nhà xuất bản để đổ vào form dropdown.
     */
    @Transactional(readOnly = true)
    public List<Publisher> getAllPublishers() {
        return publisherRepository.findAllByOrderByNameAsc();
    }

    /**
     * Lấy chi tiết đầu sách theo ID.
     */
    @Transactional(readOnly = true)
    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đầu sách với mã: " + id));
    }

    /**
     * Lấy dữ liệu DTO từ Entity để đổ vào Form chỉnh sửa.
     */
    @Transactional(readOnly = true)
    public BookForm getBookForm(Long id) {
        Book book = getBookById(id);
        BookForm form = new BookForm();
        form.setId(book.getId());
        form.setIsbn(book.getIsbn());
        form.setTitle(book.getTitle());
        form.setPublisher(book.getPublisher());
        form.setPublishYear(book.getPublishYear());
        form.setDescription(book.getDescription());
        form.setCoverUrl(book.getCoverUrl());
        form.setVisible(book.isVisible());
        if (book.getCategory() != null) {
            form.setCategoryId(book.getCategory().getId());
        }
        String authors = book.getAuthors().stream()
                .map(Author::getName)
                .collect(Collectors.joining(", "));
        form.setAuthorNames(authors);
        return form;
    }

    /**
     * Thêm mới đầu sách (Kiểm tra trùng ISBN theo quy định).
     */
    @Transactional
    public void createBook(BookForm form) {
        validateForm(form);

        String cleanIsbn = form.getIsbn().trim();
        // Kiểm tra quy tắc trùng ISBN (UC20)
        if (bookRepository.existsByIsbn(cleanIsbn)) {
            throw new BusinessException("Mã ISBN [" + cleanIsbn + "] đã tồn tại trong hệ thống. "
                    + "Quy định: Với sách trùng ISBN, vui lòng bổ sung bản sao vật lý (UC21) thay vì tạo đầu sách mới!");
        }

        Book book = new Book();
        book.setIsbn(cleanIsbn);
        applyFormData(book, form);
        bookRepository.save(book);
    }

    /**
     * Cập nhật thông tin đầu sách.
     */
    @Transactional
    public void updateBook(Long id, BookForm form) {
        Book book = getBookById(id);
        validateForm(form);

        String cleanIsbn = form.getIsbn().trim();
        if (bookRepository.existsByIsbnAndIdNot(cleanIsbn, id)) {
            throw new BusinessException("Mã ISBN [" + cleanIsbn + "] đã thuộc về một đầu sách khác.");
        }

        book.setIsbn(cleanIsbn);
        applyFormData(book, form);
        bookRepository.save(book);
    }

    /**
     * Đổi trạng thái hiển thị (Soft-delete / Toggle visible).
     */
    @Transactional
    public void toggleVisibility(Long id) {
        Book book = getBookById(id);
        book.setVisible(!book.isVisible());
        bookRepository.save(book);
    }

    private void validateForm(BookForm form) {
        if (form.getIsbn() == null || form.getIsbn().trim().isEmpty()) {
            throw new BusinessException("Mã ISBN không được để trống");
        }
        String cleanIsbn = form.getIsbn().trim();
        if (cleanIsbn.length() < 10 || cleanIsbn.length() > 20) {
            throw new BusinessException("Mã ISBN phải từ 10 đến 20 ký tự");
        }
        if (form.getTitle() == null || form.getTitle().trim().isEmpty()) {
            throw new BusinessException("Tên sách không được để trống");
        }
        if (form.getTitle().trim().length() > 255) {
            throw new BusinessException("Tên sách không được vượt quá 255 ký tự");
        }
        if (form.getAuthorNames() == null || form.getAuthorNames().trim().isEmpty()) {
            throw new BusinessException("Vui lòng nhập ít nhất một tác giả");
        }
    }

    private void applyFormData(Book book, BookForm form) {
        book.setTitle(form.getTitle().trim());
        book.setPublisher(ValidationUtil.trim(form.getPublisher()));
        book.setPublishYear(form.getPublishYear());
        book.setDescription(ValidationUtil.trim(form.getDescription()));
        book.setCoverUrl(ValidationUtil.trim(form.getCoverUrl()).isEmpty() ? null : ValidationUtil.trim(form.getCoverUrl()));
        book.setVisible(form.isVisible());

        // Gán Category
        if (form.getCategoryId() != null) {
            Category category = categoryRepository.findById(form.getCategoryId())
                    .orElse(null);
            book.setCategory(category);
        } else {
            book.setCategory(null);
        }

        // Xử lý tách chuỗi tác giả và lưu
        Set<Author> authors = resolveAuthors(form.getAuthorNames());
        book.setAuthors(authors);
    }

    private Set<Author> resolveAuthors(String authorNames) {
        Set<Author> result = new LinkedHashSet<>();
        if (authorNames == null || authorNames.trim().isEmpty()) {
            return result;
        }

        String[] names = authorNames.split("[,;]");
        for (String rawName : names) {
            String name = rawName.trim();
            if (!name.isEmpty()) {
                Author author = authorRepository.findByNameIgnoreCase(name)
                        .orElseGet(() -> {
                            Author newAuthor = new Author();
                            newAuthor.setName(name);
                            return authorRepository.save(newAuthor);
                        });
                result.add(author);
            }
        }
        return result;
    }
}
