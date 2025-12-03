package com.artemhontar.fluxdigitalstore.service;

import com.artemhontar.fluxdigitalstore.api.model.Book.BookFilter;
import com.artemhontar.fluxdigitalstore.api.model.Book.BookSpecification;
import com.artemhontar.fluxdigitalstore.model.Author;
import com.artemhontar.fluxdigitalstore.model.Book;
import com.artemhontar.fluxdigitalstore.model.Category;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookSpecificationTest {

    @Mock
    private Root<Book> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder builder;

    private BookFilter filter;
    private BookSpecification spec;

    // --- Mock Path and Join Objects ---
    @Mock
    private Path<String> stringPath;
    @Mock
    private Path<BigDecimal> bigDecimalPath;
    @Mock
    private Path<Integer> integerPath;

    // Join objects, correctly typed
    @Mock
    private Join<Book, Author> authorJoin;
    @Mock
    private Join<Book, Category> categoryJoin;

    // Path objects for the joined entities
    @Mock
    private Path<String> authorNamePath;
    @Mock
    private Path<String> categoryNamePath;

    // Mock the expression used for LOWER(field)
    @Mock
    private Expression<String> lowerCaseExpression;

    // Mock Predicate
    @Mock
    private Predicate mockPredicate;


    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // --- 1. Mock root.get() for standard fields (eq() MUST remain in when() for mix-and-match arguments) ---
        lenient().when(root.get(eq("title"))).thenReturn((Path) stringPath);
        lenient().when(root.get(eq("isbn"))).thenReturn((Path) stringPath);
        lenient().when(root.get(eq("category"))).thenReturn((Path) stringPath);

        // Fields returning specific types. (This is where the warning occurred)
        lenient().when(root.get(eq("price"))).thenReturn((Path) bigDecimalPath);
        lenient().when(root.get(eq("publicationYear"))).thenReturn((Path) integerPath);

        // --- 2. Mock root.join() for collection fields ---
        lenient().when(root.join(eq("authors"), eq(JoinType.INNER))).thenReturn((Join) authorJoin);
        lenient().when(root.join(eq("categories"), eq(JoinType.INNER))).thenReturn((Join) categoryJoin);

        // --- 3. Mock path extraction after the join ---
        lenient().when(authorJoin.get(eq("name"))).thenReturn((Path) authorNamePath);
        lenient().when(categoryJoin.get(eq("name"))).thenReturn((Path) categoryNamePath);

        // --- 4. Mock builder.lower() to return a usable expression ---
        lenient().when(builder.lower(eq(stringPath))).thenReturn(lowerCaseExpression);
        lenient().when(builder.lower(eq(authorNamePath))).thenReturn(lowerCaseExpression);
        lenient().when(builder.lower(eq(categoryNamePath))).thenReturn(lowerCaseExpression);

        // --- 5. Mock all predicate generation methods ---
        lenient().when(builder.like(any(Expression.class), anyString())).thenReturn(mockPredicate);
        lenient().when(builder.equal(any(Expression.class), anyString())).thenReturn(mockPredicate);
        lenient().when(builder.greaterThanOrEqualTo(any(Path.class), any(BigDecimal.class))).thenReturn(mockPredicate);
        lenient().when(builder.lessThanOrEqualTo(any(Path.class), any(BigDecimal.class))).thenReturn(mockPredicate);
        lenient().when(builder.greaterThanOrEqualTo(any(Path.class), any(Integer.class))).thenReturn(mockPredicate);
        lenient().when(builder.lessThanOrEqualTo(any(Path.class), any(Integer.class))).thenReturn(mockPredicate);

        // Mock the final AND combination
        lenient().when(builder.and(any(Predicate[].class))).thenReturn(mockPredicate);
    }

    // ===================================
    // TEST: All Filters Active
    // ===================================

    @Test
    void toPredicate_AllFieldsSet_AppliesAllPredicatesAndJoins() {
        // ARRANGE: FIX - Instantiate BookFilter using no-arg constructor and setters
        filter = new BookFilter();
        filter.setTitle("Test Title");
        filter.setAuthor("Test Author"); // Corresponds to the Author join logic
        filter.setIsbn("9781234567897");
        filter.setMinPrice(new BigDecimal("10.00"));
        filter.setMaxPrice(new BigDecimal("50.00"));
        filter.setMinPublicationYear(2000);
        filter.setMaxPublicationYear(2020);
        filter.setCategory("Science"); // Corresponds to the Category join logic

        spec = new BookSpecification(filter);

        // ACT
        spec.toPredicate(root, query, builder);

        // ASSERT: Verification calls (checking logic, not return value)
        // Verify Joins and Distinct
        verify(root, times(1)).join("authors", JoinType.INNER);
        verify(root, times(1)).join("categories", JoinType.INNER);
        verify(query, times(1)).distinct(true);

        // Verify Predicate Calls:
        verify(builder, times(1)).like(lowerCaseExpression, "%test title%");
        verify(builder, times(1)).equal(lowerCaseExpression, "9781234567897");

        // Price Range
        verify(builder, times(1)).greaterThanOrEqualTo(bigDecimalPath, new BigDecimal("10.00"));
        verify(builder, times(1)).lessThanOrEqualTo(bigDecimalPath, new BigDecimal("50.00"));

        // Year Range
        verify(builder, times(1)).greaterThanOrEqualTo(integerPath, 2000);
        verify(builder, times(1)).lessThanOrEqualTo(integerPath, 2020);

        // Joined Entity Filters
        verify(builder, times(1)).like(lowerCaseExpression, "%test author%");
        verify(builder, times(1)).like(lowerCaseExpression, "%science%");

        verify(builder, times(1)).and(any(Predicate[].class));
    }

    // ===================================
    // TEST: Individual String Filters
    // ===================================

    @Test
    void toPredicate_OnlyTitleSet_UsesLowerCaseLike() {
        // ARRANGE: FIX - Using no-argument constructor and setter
        filter = new BookFilter();
        filter.setTitle("Test Title");
        spec = new BookSpecification(filter);

        // ACT
        spec.toPredicate(root, query, builder);

        // ASSERT
        verify(builder, times(1)).like(lowerCaseExpression, "%test title%");
        verify(builder, never()).equal(any(Expression.class), anyString());
        verify(root, never()).join(anyString(), any());
        verify(root, never()).join(anyString(), any());
    }

    @Test
    void toPredicate_OnlyISBNSet_UsesLowerCaseEqual() {
        // ARRANGE: FIX - Using no-argument constructor and setter
        filter = new BookFilter();
        filter.setIsbn("9781234567897");
        spec = new BookSpecification(filter);

        // ACT
        spec.toPredicate(root, query, builder);

        // ASSERT
        verify(builder, times(1)).equal(lowerCaseExpression, "9781234567897");
        verify(builder, never()).like(any(Expression.class), anyString());
    }

    @Test
    void toPredicate_OnlyAuthorSet_UsesInnerJoinAndLowerCaseLike() {
        // ARRANGE: FIX - Using no-argument constructor and setter
        filter = new BookFilter();
        filter.setAuthor("Test Author");
        spec = new BookSpecification(filter);

        // ACT
        spec.toPredicate(root, query, builder);

        // ASSERT
        verify(root, times(1)).join("authors", JoinType.INNER);
        verify(builder, times(1)).like(lowerCaseExpression, "%test author%");
        verify(query, times(0)).distinct(true); // Should not call distinct unless categories are involved or implicitly in the spec logic
    }

    @Test
    void toPredicate_OnlyCategorySet_UsesInnerJoinLowerCaseLikeAndDistinct() {
        // ARRANGE: FIX - Using no-argument constructor and setter
        filter = new BookFilter();
        filter.setCategory("Test Category");
        spec = new BookSpecification(filter);

        // ACT
        spec.toPredicate(root, query, builder);

        // ASSERT
        verify(root, times(1)).join("categories", JoinType.INNER);
        verify(builder, times(1)).like(lowerCaseExpression, "%test category%");
        verify(query, times(1)).distinct(true);
    }

    // ===================================
    // TEST: Range Filters
    // ===================================

    @Test
    void toPredicate_PriceRangeFilter_AppliesGTEAndLTE() {
        // ARRANGE: FIX - Using no-argument constructor and setter
        filter = new BookFilter();
        filter.setMinPrice(new BigDecimal("10.00"));
        filter.setMaxPrice(new BigDecimal("50.00"));
        spec = new BookSpecification(filter);

        // ACT
        spec.toPredicate(root, query, builder);

        // ASSERT
        verify(builder, times(1)).greaterThanOrEqualTo(bigDecimalPath, new BigDecimal("10.00"));
        verify(builder, times(1)).lessThanOrEqualTo(bigDecimalPath, new BigDecimal("50.00"));
        verify(builder, times(1)).and(any(Predicate[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
        // Suppress warning on raw Path type used in 'never()' check
    void toPredicate_MinYearFilterOnly_AppliesGTE() {
        filter = new BookFilter();
        filter.setMinPublicationYear(2000);
        spec = new BookSpecification(filter);

        // ACT
        spec.toPredicate(root, query, builder);

        // ASSERT
        // Refactored: Removed redundant eq() invocations.
        verify(builder, times(1)).greaterThanOrEqualTo(integerPath, 2000);
        verify(builder, never()).lessThanOrEqualTo(any(Path.class), any(Integer.class));
    }


    // ===================================
    // TEST: Edge Cases (Null/Blank)
    // ===================================

    @Test
    void toPredicate_NoFiltersSet_ReturnsCombinedPredicate() {
        // ARRANGE: FIX - Using no-argument constructor
        filter = new BookFilter();
        spec = new BookSpecification(filter);

        // ACT
        spec.toPredicate(root, query, builder);

        // ASSERT
        // The implementation returns cb.and(predicates.toArray(new Predicate[0])), which is a TRUE predicate.
        verify(builder, times(1)).and(new Predicate[0]); // eq(new Predicate[0]) replaced with direct argument
        verify(builder, never()).like(any(Expression.class), anyString());
    }

    @Test
    void toPredicate_BlankStringFilters_AreIgnored() {
        // ARRANGE: FIX - Using no-argument constructor
        filter = new BookFilter();
        filter.setTitle("  ");
        filter.setAuthor(" ");
        filter.setIsbn(" ");
        filter.setCategory(" ");
        spec = new BookSpecification(filter);

        // ACT
        spec.toPredicate(root, query, builder);

        // ASSERT
        verify(builder, times(1)).and(any(Predicate[].class));
        verify(builder, never()).like(any(Expression.class), anyString());
        verify(builder, never()).equal(any(Expression.class), anyString());
        verify(root, never()).join(anyString(), any());
    }
}