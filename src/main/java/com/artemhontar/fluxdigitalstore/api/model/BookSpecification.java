package com.artemhontar.fluxdigitalstore.api.model;

import com.artemhontar.fluxdigitalstore.model.Book;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public record BookSpecification(BookFilter filter) implements Specification<Book> {

    @Override
    public Predicate toPredicate(Root<Book> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        // Access the record component using filter()
        // 1. Title Filter (Case-insensitive LIKE)
        if (filter.getTitle() != null && !filter.getTitle().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("title")), "%" + filter.getTitle().toLowerCase() + "%"));
        }

        // 2. ISBN Filter (Exact Match)
        if (filter.getIsbn() != null && !filter.getIsbn().isBlank()) {
            predicates.add(cb.equal(cb.lower(root.get("isbn")), filter.getIsbn().toLowerCase()));
        }

        // 3. Price Range Filter
        if (filter.getMinPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
        }
        if (filter.getMaxPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
        }

        // 4. Publication Year Filter
        if (filter.getMaxPublicationYear() != null) {
            // WHERE publicationYear <= maxPublicationYear
            predicates.add(cb.lessThanOrEqualTo(root.get("publicationYear"), filter.getMaxPublicationYear()));
        }

        // 5. Author Filter (Requires a JOIN, assuming 'authors' is a ManyToMany collection)
        if (filter.getAuthor() != null && !filter.getAuthor().isBlank()) {
            // Join the 'authors' collection
            Join<Book, Object> authorJoin = root.join("authors", JoinType.INNER);

            // Search author name (e.g., name LIKE '%author%')
            predicates.add(cb.like(cb.lower(authorJoin.get("name")), "%" + filter.getAuthor().toLowerCase() + "%"));
        }

        // 6. Category Filter (Requires a JOIN, assuming 'categories' is a ManyToMany collection)
        if (filter.getCategory() != null && !filter.getCategory().isBlank()) {
            // Join the 'categories' collection on the Book entity
            Join<Book, Object> categoryJoin = root.join("categories", JoinType.INNER);

            // Search for the category name (e.g., name LIKE '%Gothic%')
            predicates.add(cb.like(cb.lower(categoryJoin.get("name")), "%" + filter.getCategory().toLowerCase() + "%"));

            // Set distinct to true to prevent duplicate books when joining collections
            query.distinct(true);
        }

        // Combine all predicates with an AND operator
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}