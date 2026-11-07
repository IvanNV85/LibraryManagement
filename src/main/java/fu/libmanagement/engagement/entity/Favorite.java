package fu.libmanagement.engagement.entity;

import fu.libmanagement.catalog.entity.Book;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorites")
@Getter
@Setter
@NoArgsConstructor
public class Favorite {

    @EmbeddedId
    private FavoriteId id;


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