package fu.libmanagement.catalog.service;

import fu.libmanagement.catalog.dto.BookSummary;
import fu.libmanagement.catalog.dto.SearchField;
import fu.libmanagement.catalog.entity.BookSort;
import fu.libmanagement.catalog.entity.Category;
import org.springframework.data.domain.Page;

import java.util.List;


public interface BookFilterService {

    Page<BookSummary> search(String keyword, SearchField field, long categoryId, int year,
                             BookSort sort, int page);

    List<Category> getCategories();

    List<Integer> getYears();
}
