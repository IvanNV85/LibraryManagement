package fu.libmanagement.engagement.dto;

import fu.libmanagement.engagement.entity.Review;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private String readerName;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getReader().getFullName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }
}