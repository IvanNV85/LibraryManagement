package fu.libmanagement.catalog.service;

import fu.libmanagement.catalog.entity.Book;
import fu.libmanagement.catalog.repository.BookRepository;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.common.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Cập nhật ảnh bìa cho đầu sách (dùng lại FileStorageService có sẵn). */
@Service
@RequiredArgsConstructor
public class BookCoverService {

    private final BookRepository bookRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public Book getBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đầu sách"));
    }

    /** Lưu ảnh mới vào uploads/covers, cập nhật cover_url và xóa ảnh cũ (nếu có). */
    @Transactional
    public Book updateCover(Long bookId, MultipartFile cover) {
        Book book = getBook(bookId);
        String oldCover = book.getCoverUrl();
        book.setCoverUrl(fileStorageService.saveImage(cover, "covers")); // Hibernate tự UPDATE khi commit
        fileStorageService.delete(oldCover);
        return book;
    }
}
