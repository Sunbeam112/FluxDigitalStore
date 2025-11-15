package com.artemhontar.fluxdigitalstore.repo;

import com.artemhontar.fluxdigitalstore.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface BookRepo extends JpaRepository<Book, Long> {

    boolean existsByIsbnIgnoreCase(String isbn);

    Book getByIsbnIgnoreCase(String isbn);

    @Override
    boolean existsById(Long Id);

    Page<Book> getByTitleIgnoreCaseAllIgnoreCase(String title, Pageable pageable);

    Page<Book> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    @Query("SELECT b FROM Book b JOIN b.authors a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :authorName, '%'))")
    Page<Book> findByAuthorContainingCaseInsensitive(@Param("authorName") String authorName, Pageable pageable);
}