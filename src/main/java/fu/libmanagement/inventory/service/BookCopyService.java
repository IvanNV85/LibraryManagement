package fu.libmanagement.inventory.service;

import fu.libmanagement.catalog.entity.Book;
import fu.libmanagement.inventory.entity.BookCopy;
import fu.libmanagement.inventory.entity.CopyStatus;
import fu.libmanagement.inventory.repository.BookCopyRepository;
import fu.libmanagement.catalog.repository.BookRepository;
import fu.libmanagement.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BookCopyService {
    private final BookCopyRepository copies;
    private final BookRepository books;

    @Transactional(readOnly = true)
    public Page<BookCopy> search(String keyword, String status, int page) {
        CopyStatus filter = null;
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            try { filter = CopyStatus.valueOf(status); }
            catch (IllegalArgumentException e) { throw new BusinessException("Trạng thái không hợp lệ."); }
        }
        String term = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return copies.search("%" + term + "%", filter, PageRequest.of(Math.max(0, page), 20, Sort.by("id").descending()));
    }

    @Transactional(readOnly = true)
    public BookCopy detail(Long id) {
        return copies.findWithBook(id).orElseThrow(() -> new BusinessException("Không tìm thấy bản sao."));
    }

    @Transactional(readOnly = true)
    public List<Book> allBooks() { return books.findAll(Sort.by("title")); }

    @Transactional
    public Long create(Long bookId, String barcode, String shelf, BigDecimal price, String note) {
        String code = required(barcode, 30, "Barcode");
        if (copies.existsByBarcode(code)) throw new BusinessException("Barcode đã tồn tại.");
        Book book = bookId == null ? null : books.findById(bookId).orElse(null);
        if (book == null) throw new BusinessException("Vui lòng chọn đầu sách hợp lệ.");
        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setBarcode(code);
        setDetails(copy, shelf, price, note);
        try { return copies.saveAndFlush(copy).getId(); }
        catch (DataIntegrityViolationException e) { throw new BusinessException("Không thể tạo bản sao; barcode có thể đã tồn tại."); }
    }

    @Transactional
    public void update(Long id, String shelf, BigDecimal price, String note) {
        BookCopy copy = locked(id);
        setDetails(copy, shelf, price, note);
    }

    @Transactional
    public void recordCondition(Long id, String status) {
        CopyStatus target;
        try { target = CopyStatus.valueOf(status); }
        catch (IllegalArgumentException | NullPointerException e) { throw new BusinessException("Trạng thái không hợp lệ."); }
        BookCopy copy = locked(id);
        CopyStatus current = copy.getStatus();
        boolean permitted = (current == CopyStatus.AVAILABLE && (target == CopyStatus.DAMAGED || target == CopyStatus.LOST || target == CopyStatus.DISPOSED))
                || ((current == CopyStatus.DAMAGED || current == CopyStatus.LOST) && target == CopyStatus.DISPOSED);
        if (!permitted) throw new BusinessException("Không thể chuyển trạng thái bản sao theo yêu cầu.");
        if (copies.countActiveLoans(id) > 0 || copies.countActiveReservations(id) > 0)
            throw new BusinessException("Bản sao đang liên quan đến lượt mượn hoặc đặt trước còn hiệu lực.");
        copy.setStatus(target);
    }

    private BookCopy locked(Long id) {
        return copies.findForUpdate(id).orElseThrow(() -> new BusinessException("Không tìm thấy bản sao."));
    }

    private void setDetails(BookCopy copy, String shelf, BigDecimal price, String note) {
        copy.setShelfLocation(optional(shelf, 50, "Vị trí kệ"));
        copy.setNote(optional(note, 255, "Ghi chú"));
        if (price != null && (price.signum() < 0 || price.compareTo(new BigDecimal("9999999999.99")) > 0 || price.stripTrailingZeros().scale() > 2))
            throw new BusinessException("Giá trị phải không âm, tối đa 10 chữ số nguyên và 2 chữ số thập phân.");
        copy.setPrice(price);
    }

    private String required(String value, int max, String field) {
        String result = optional(value, max, field);
        if (result == null) throw new BusinessException(field + " không được để trống.");
        return result;
    }

    private String optional(String value, int max, String field) {
        if (value == null || value.isBlank()) return null;
        String result = value.trim();
        if (result.length() > max) throw new BusinessException(field + " quá dài (tối đa " + max + " ký tự).");
        return result;
    }
}
