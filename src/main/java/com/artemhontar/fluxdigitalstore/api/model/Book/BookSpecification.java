package com.artemhontar.fluxdigitalstore.api.model.Book;

import com.artemhontar.fluxdigitalstore.model.Book;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code BookSpecification} is a record implementation of the Spring Data JPA {@link Specification}
 * interface for the {@link Book} entity.
 * <p>
 * This class translates criteria provided in a {@link BookFilter} object into JPA Criteria API
 * {@link Predicate}s, allowing for dynamic and flexible database querying. All non-null and non-blank
 * filter criteria are combined using the logical AND operator.
 * </p>
 *
 * @param filter The filter object containing the criteria (e.g., title, price range, years).
 * @author Artem Hontar
 */
public record BookSpecification(BookFilter filter) implements Specification<Book> {

    /**
     * Creates a {@link Predicate} from the provided {@link BookFilter} criteria.
     * All predicates generated from the filter fields are combined using a logical AND.
     *
     * @param root  The root path in the FROM clause, representing the {@link Book} entity.
     * @param query The {@link CriteriaQuery} for constructing the query structure.
     * @param cb    The {@link CriteriaBuilder} for constructing individual predicates.
     * @return A single combined {@link Predicate}, or {@code null} if no filters are applied.
     */
    @Override
    public Predicate toPredicate(Root<Book> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        // 1. Title Filter (Case-insensitive LIKE: WHERE LOWER(title) LIKE '%title%')
        if (filter.getTitle() != null && !filter.getTitle().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("title")), "%" + filter.getTitle().toLowerCase() + "%"));
        }

        // 2. ISBN Filter (Case-insensitive Exact Match: WHERE LOWER(isbn) = 'isbn')
        if (filter.getIsbn() != null && !filter.getIsbn().isBlank()) {
            predicates.add(cb.equal(cb.lower(root.get("isbn")), filter.getIsbn().toLowerCase()));
        }

        // 3. Price Range Filter
        if (filter.getMinPrice() != null) {
            // WHERE price >= minPrice
            predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
        }
        if (filter.getMaxPrice() != null) {
            // WHERE price <= maxPrice
            predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
        }

        // 4. Publication Year Range Filter
        if (filter.getMinPublicationYear() != null) {
            // WHERE publicationYear >= minPublicationYear
            predicates.add(cb.greaterThanOrEqualTo(root.get("publicationYear"), filter.getMinPublicationYear()));
        }

        if (filter.getMaxPublicationYear() != null) {
            // WHERE publicationYear <= maxPublicationYear
            predicates.add(cb.lessThanOrEqualTo(root.get("publicationYear"), filter.getMaxPublicationYear()));
        }

        // 5. Author Filter (Many-to-Many Join: INNER JOIN authors ON ... WHERE LOWER(authors.name) LIKE '%author%')
        if (filter.getAuthor() != null && !filter.getAuthor().isBlank()) {
            // Join the 'authors' collection (assuming the field name is "authors" on the Book entity)
            Join<Book, Object> authorJoin = root.join("authors", JoinType.INNER);

            // Search author name
            predicates.add(cb.like(cb.lower(authorJoin.get("name")), "%" + filter.getAuthor().toLowerCase() + "%"));
        }

        // 6. Category Filter (Many-to-Many Join: INNER JOIN categories ON ... WHERE LOWER(categories.name) LIKE '%category%')
        if (filter.getCategory() != null && !filter.getCategory().isBlank()) {
            // Join the 'categories' collection on the Book entity (assuming the field name is "categories")
            Join<Book, Object> categoryJoin = root.join("categories", JoinType.INNER);

            // Search for the category name
            predicates.add(cb.like(cb.lower(categoryJoin.get("name")), "%" + filter.getCategory().toLowerCase() + "%"));

            // Set distinct to true to prevent duplicate Book results when joining collections
            query.distinct(true);
        }

        // Combine all predicates with an AND operator
        // If predicates is empty, cb.and() with no arguments typically returns true (all rows match).
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}