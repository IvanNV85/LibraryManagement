package fu.libmanagement.engagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/** Khóa chính ghép của bảng favorites. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FavoriteId implements Serializable {

    @Column(name = "reader_id")
    private Long readerId;

    @Column(name = "book_id")
    private Long bookId;
}
