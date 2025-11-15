package com.artemhontar.fluxdigitalstore.service;

import com.artemhontar.fluxdigitalstore.api.model.BookRequest;
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

@Component
@RequiredArgsConstructor
public class BookConverter {

    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;

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

    public String normalizeISBN(String isbn) {
        //Remove white spaces and hyphens
        return isbn.replaceAll("[\\s-]+", "");
    }


}