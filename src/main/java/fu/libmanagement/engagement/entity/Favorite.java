package fu.libmanagement.engagement.entity;

import fu.libmanagement.catalog.entity.Book;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Một sách trong danh sách yêu thích của một độc giả (bảng favorites). */
@Entity
@Table(name = "favorites")
@Getter
@Setter
@NoArgsConstructor
public class Favorite {

    @EmbeddedId
    private FavoriteId id;

    /** book_id vừa là khóa chính vừa là khóa ngoại: @MapsId lấy giá trị từ id.bookId. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("bookId")
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Favorite(Long readerId, Book book) {
        this.id = new FavoriteId(readerId, book.getId());
        this.book = book;
    }
}