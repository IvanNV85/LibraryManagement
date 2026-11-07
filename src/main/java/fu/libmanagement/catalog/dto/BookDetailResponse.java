package fu.libmanagement.catalog.dto;

import lombok.*;

//UC8:
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookDetailResponse {

    private Long id;
    private String title;
    private String isbn;
    private String publisher;
    private Integer publishYear;
    private String description;
    private String coverUrl;
    private String categoryName;
    private String authorNames;

    private int totalCopies;
    private int availableCopies;
    private String shelfLocations;

    private Double averageRating;
    private long reviewCount;
    private boolean favorite;
}
