package fu.libmanagement.engagement.service;

import fu.libmanagement.engagement.dto.ReviewRequest;

/** UC10 Đánh giá, bình luận sách. */
public interface ReviewService {
    //Độc giả mượn sách mới đc đánh giá
    boolean canReview(Long readerId, Long bookId);

    //Đánh giá sẵn có của độc giả
    ReviewRequest getMyReview(Long readerId, Long bookId);

    //Tạo mới đánh gia
    void save(Long readerId, Long bookId, ReviewRequest request);
}
