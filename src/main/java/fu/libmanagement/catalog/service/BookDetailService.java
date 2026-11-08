package fu.libmanagement.catalog.service;

import fu.libmanagement.catalog.dto.BookDetailResponse;
import fu.libmanagement.engagement.dto.ReviewResponse;

import java.util.List;

//UC8: xem chi tiết sách
public interface BookDetailService {

    BookDetailResponse getDetail(Long bookId, Long readerId);

    List<ReviewResponse> getLatestReviews(Long bookId);
}