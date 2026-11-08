package fu.libmanagement.catalog.repository;

import fu.libmanagement.catalog.entity.Book;
import fu.libmanagement.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface BookFilterRepository extends JpaRepository<Book, Long> {


    List<Book> findByVisibleTrue();


    @Query("select c from Category c order by c.name")
    List<Category> findCategories();


    @Query("select distinct b.publishYear from Book b "
            + "where b.visible = true and b.publishYear is not null order by b.publishYear desc")
    List<Integer> findYears();
}
