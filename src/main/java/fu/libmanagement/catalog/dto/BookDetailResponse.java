package fu.libmanagement.catalog.dto;

import lombok.Builder;
import lombok.Getter;

//UC8: Chi tiết sách(Dữ liêu)
@Getter
@Builder
public class BookDetailResponse {

    private final Long id;
    private final String title;
    private final String isbn;
    private final String publisher;
    private final Integer publishYear;
    private final String description;
    private final String coverUrl;
    private final String categoryName;
    private final String authorNames;

    private final int totalCopies;
    private final int availableCopies;
    private final String shelfLocations;

    private final Double averageRating;
    private final long reviewCount;
    private final boolean favorite;
}
