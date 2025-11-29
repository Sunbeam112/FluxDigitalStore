package com.artemhontar.fluxdigitalstore.service.Books;

import com.artemhontar.fluxdigitalstore.api.model.Book.BookDTO;
import com.artemhontar.fluxdigitalstore.api.model.Book.BookRequest;
import com.artemhontar.fluxdigitalstore.model.Author;
import com.artemhontar.fluxdigitalstore.model.Book;
import com.artemhontar.fluxdigitalstore.model.Category;
import com.artemhontar.fluxdigitalstore.repo.AuthorRepository;
import com.artemhontar.fluxdigitalstore.repo.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converter component responsible for transforming data between {@link BookRequest} (client DTO),
 * {@link Book} (JPA Entity), and {@link BookDTO} (API response DTO).
 * <p>
 * This class handles database lookups for related entities (Authors, Categories) during
 * conversion from DTO to Entity, creating new Author records if they don't exist.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class BookConverter {

    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;

    /**
     * Converts a {@link BookRequest} DTO into a {@link Book} entity.
     * This operation is transactional because it may involve creating new {@link Author} entities
     * and fetching existing {@link Category} entities from the database.
     *
     * @param request The {@link BookRequest} containing book data from the client.
     * @return The resulting {@link Book} entity ready for persistence.
     */
    @Transactional
    public Book toEntity(BookRequest request) {

        Book book = Book.builder()
                .title(request.getTitle())
                .isbn(normalizeISBN(request.getIsbn()))
                .price(request.getPrice())
                .publicationYear(request.getPublicationYear())
                .description(request.getDescription())
                .shortDescription(request.getShortDescription())
                .category(request.getCategory())
                .subcategory(request.getSubcategory())
                .urlPhoto(request.getUrlPhoto())
                .build();

        // Handle Authors: find existing or create new ones
        if (request.getAuthorNames() != null && !request.getAuthorNames().isEmpty()) {
            Set<Author> authors = request.getAuthorNames().stream()
                    .map(name -> authorRepository.findByName(name)
                            .orElseGet(() -> {
                                Author newAuthor = new Author(name);
                                return authorRepository.save(newAuthor);
                            }))
                    .collect(Collectors.toSet());

            book.setAuthors(authors);
        } else {
            book.setAuthors(new HashSet<>());
        }


        // Handle Categories: fetch existing categories by ID
        if (request.getCategoryIds() != null) {
            Set<Category> categories = request.getCategoryIds().stream()
                    .map(id -> categoryRepository.findById(id).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            book.setCategories(categories);
        } else {
            book.setCategories(new HashSet<>());
        }

        return book;
    }

    /**
     * Removes whitespace and hyphens from an ISBN string to create a normalized, clean version.
     *
     * @param isbn The raw ISBN string, possibly containing spaces and hyphens.
     * @return The normalized ISBN string.
     */
    public String normalizeISBN(String isbn) {
        //Remove white spaces and hyphens
        return isbn.replaceAll("[\\s-]+", "");
    }

    /**
     * Converts a {@link Book} entity into a {@link BookDTO} for API response.
     * This flattens the related entity sets (Authors, Categories) into simple name strings.
     *
     * @param book The {@link Book} entity retrieved from the database.
     * @return The resulting {@link BookDTO} for the client.
     */
    public BookDTO toDto(Book book) {
        Set<String> authorNames = book.getAuthors() != null
                ? book.getAuthors().stream().map(Author::getName).collect(Collectors.toSet())
                : Set.of();

        Set<String> categoryNames = book.getCategories() != null
                ? book.getCategories().stream().map(Category::getName).collect(Collectors.toSet())
                : Set.of();

        return BookDTO.builder()
                .id(book.getId())
                .title(book.getTitle())
                .authorNames(authorNames)
                .isbn(book.getIsbn())
                .price(book.getPrice())
                .publicationYear(book.getPublicationYear())
                .shortDescription(book.getShortDescription())
                .category(book.getCategory())
                .subcategory(book.getSubcategory())
                .categoryNames(categoryNames)
                .urlPhoto(book.getUrlPhoto())
                .build();
    }


}