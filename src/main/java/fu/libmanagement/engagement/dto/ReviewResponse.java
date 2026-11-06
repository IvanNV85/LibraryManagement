package fu.libmanagement.engagement.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

//Đánh giá hiển thị trong chi tiết sách
@Getter
@RequiredArgsConstructor
public class ReviewResponse {

    private final String readerName;
    private final int rating;
    private final String comment;
    private final LocalDateTime createdAt;
}
