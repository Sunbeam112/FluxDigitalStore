package com.artemhontar.fluxdigitalstore.repo;

import com.artemhontar.fluxdigitalstore.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
// 🚀 ADDING JpaSpecificationExecutor<Book>
public interface BookRepo extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    boolean existsByIsbnIgnoreCase(String isbn);

    Book findByIsbnIgnoreCase(String isbn);

    @Override
    boolean existsById(Long Id);

    @Query("SELECT b FROM Book b JOIN b.authors a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :authorName, '%'))")
    Page<Book> findByAuthorContainingCaseInsensitive(@Param("authorName") String authorName, Pageable pageable);
}