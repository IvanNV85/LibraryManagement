package fu.libmanagement.engagement.service;

import fu.libmanagement.catalog.entity.Book;
import fu.libmanagement.catalog.repository.BookRepository;
import fu.libmanagement.common.exception.BusinessException;
import fu.libmanagement.engagement.dto.ReviewRequest;
import fu.libmanagement.engagement.entity.Review;
import fu.libmanagement.engagement.repository.ReviewRepository;
import fu.libmanagement.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final int MAX_COMMENT_LENGTH = 1000;

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public boolean canReview(Long readerId, Long bookId) {
        return reviewRepository.countBorrowed(readerId, bookId) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewRequest getMyReview(Long readerId, Long bookId) {
        ReviewRequest form = new ReviewRequest();
        reviewRepository.findByBookIdAndReaderId(bookId, readerId).ifPresent(review -> {
            form.setRating(review.getRating());
            form.setComment(review.getComment());
        });
        return form;
    }

    @Override
    @Transactional
    public void save(Long readerId, Long bookId, ReviewRequest request) {
        // 1. Kiểm tra dữ liệu gửi lên (không tin form, vì ai cũng có thể tự gửi POST)
        Integer rating = request.getRating();
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException("Số sao phải từ 1 đến 5");
        }
        String comment = request.getComment() == null ? "" : request.getComment().trim();
        if (comment.length() > MAX_COMMENT_LENGTH) {
            throw new BusinessException("Nhận xét tối đa " + MAX_COMMENT_LENGTH + " ký tự");
        }

        // 2. Kiểm tra nghiệp vụ
        Book book = bookRepository.findById(bookId)
                .filter(Book::isVisible)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đầu sách"));
        if (!canReview(readerId, bookId)) {
            throw new BusinessException("Chỉ độc giả đã mượn cuốn sách này mới được đánh giá");
        }

        // 3. Chưa có thì tạo mới, có rồi thì sửa
        Review review = reviewRepository.findByBookIdAndReaderId(bookId, readerId).orElse(null);
        if (review == null) {
            review = new Review();
            review.setBook(book);
            review.setReader(userService.getUser(readerId));
        } else {
            review.setUpdatedAt(LocalDateTime.now());
        }
        review.setRating(rating);
        review.setComment(comment.isEmpty() ? null : comment);
        reviewRepository.save(review);
    }
}
