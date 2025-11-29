package com.artemhontar.fluxdigitalstore.service.Books;

import com.artemhontar.fluxdigitalstore.api.model.Book.BookFilter;
import com.artemhontar.fluxdigitalstore.api.model.Book.BookRequest;
import com.artemhontar.fluxdigitalstore.api.model.Book.BookSpecification;
import com.artemhontar.fluxdigitalstore.api.model.Book.BookDTO;
import com.artemhontar.fluxdigitalstore.exception.BookAlreadyExistsException;
import com.artemhontar.fluxdigitalstore.exception.NotFoundException;
import com.artemhontar.fluxdigitalstore.model.Book;
import com.artemhontar.fluxdigitalstore.repo.BookRepo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for managing book entities.
 * Provides methods for creating, retrieving, updating, and deleting books,
 * with support for pagination, filtering, and ISBN validation.
 */
@Service
public class BookService {

    private final BookRepo bookRepo;
    private final BookConverter bookConverter;
    private final ISBNUtil iSBNUtil;

    /**
     * Constructs the BookService with required dependencies.
     *
     * @param bookRepo      The repository for CRUD operations on Book entities.
     * @param bookConverter The utility for converting between Book entities and DTOs.
     * @param iSBNUtil      The utility for validating and normalizing ISBNs.
     */
    public BookService(BookRepo bookRepo, BookConverter bookConverter, ISBNUtil iSBNUtil) {
        this.bookRepo = bookRepo;
        this.bookConverter = bookConverter;
        this.iSBNUtil = iSBNUtil;
    }


    /**
     * Retrieves a book by its ISBN (International Standard Book Number).
     * The ISBN is validated and normalized before searching the repository.
     *
     * @param isbn The ISBN of the book to retrieve.
     * @return An Optional containing the {@link BookDTO} if found and the ISBN is valid, otherwise an empty Optional.
     */
    public Optional<BookDTO> getBookByIsbn(String isbn) {
        boolean isInputIsISBN = iSBNUtil.isValidISBN(isbn);
        if (isInputIsISBN) {
            String normalizedISBN = bookConverter.normalizeISBN(isbn);
            return Optional.ofNullable(bookRepo.findByIsbnIgnoreCase(normalizedISBN))
                    .map(bookConverter::toDto);
        }
        return Optional.empty();
    }

    /**
     * Retrieves a book by its internal database ID.
     *
     * @param id The unique ID of the book.
     * @return An Optional containing the {@link BookDTO} if found, otherwise an empty Optional.
     */
    public Optional<BookDTO> getBookByID(Long id) {
        return bookRepo.findById(id).map(bookConverter::toDto);
    }

    /**
     * Retrieves a paginated list of all books.
     *
     * @param pageable The pagination and sorting information.
     * @return A {@link Page} of {@link BookDTO} objects.
     */
    public Page<BookDTO> getAllBooksPageable(Pageable pageable) {
        return bookRepo.findAll(pageable).map(bookConverter::toDto);
    }

    /**
     * Searches for books based on a provided filter criteria and returns the results as a paginated list.
     *
     * @param filter   The criteria used to filter the books (e.g., title, author, genre).
     * @param pageable The pagination and sorting information.
     * @return A {@link Page} of {@link BookDTO} objects matching the filter.
     */
    public Page<BookDTO> searchBooksByFilter(BookFilter filter, Pageable pageable) {
        BookSpecification spec = new BookSpecification(filter);
        return bookRepo.findAll(spec, pageable).map(bookConverter::toDto);
    }


    /**
     * Creates a new Book entity. This is intended for internal use only,
     * as it accepts a {@link Book} model directly.
     *
     * @param book The Book entity to be created and persisted.
     * @return The saved Book entity.
     * @throws BookAlreadyExistsException If a book with the same ISBN already exists.
     */
    @Transactional
    public Book createBook(Book book) {
        String normalizedIsbn = bookConverter.normalizeISBN(book.getIsbn());
        checkIfBookExists(normalizedIsbn);

        book.setIsbn(normalizedIsbn);
        return bookRepo.save(book);
    }

    /**
     * Creates a new book from a client request DTO and persists it.
     *
     * @param bookRequest The request DTO containing the book data.
     * @return The created book as a {@link BookDTO}.
     * @throws BookAlreadyExistsException If a book with the same ISBN already exists.
     */
    @Transactional
    public BookDTO createBook(BookRequest bookRequest) {
        String normalizedIsbn = bookConverter.normalizeISBN(bookRequest.getIsbn());
        checkIfBookExists(normalizedIsbn);

        Book book = bookConverter.toEntity(bookRequest);
        book.setIsbn(normalizedIsbn);
        Book savedBook = bookRepo.save(book);
        return bookConverter.toDto(savedBook);
    }

    /**
     * Internal helper method to check if a book with the given normalized ISBN already exists.
     *
     * @param normalizedIsbn The ISBN string after normalization.
     * @throws BookAlreadyExistsException If a match is found in the repository.
     */
    private void checkIfBookExists(String normalizedIsbn) {
        if (bookRepo.existsByIsbnIgnoreCase(normalizedIsbn)) {
            throw new BookAlreadyExistsException("The book with ISBN " + normalizedIsbn + " already exists!");
        }
    }

    /**
     * Persists a list of book entities and returns the created entities as DTOs.
     *
     * @param books A list of {@link Book} entities to save.
     * @return An Iterable of the created books as {@link BookDTO}s.
     * @throws IllegalArgumentException If the input list is null or empty.
     */
    public Iterable<BookDTO> createAllBooksFromList(List<Book> books) {
        if (books != null && !books.isEmpty()) {
            return bookRepo.saveAll(books).stream()
                    .map(bookConverter::toDto)
                    .collect(Collectors.toList());
        }
        throw new IllegalArgumentException("List of books is empty or null!");
    }


    /**
     * Updates an existing book identified by its ID with data from the request.
     *
     * @param id          The ID of the book to update.
     * @param bookRequest The request DTO containing the updated book data.
     * @return The updated book as a {@link BookDTO}.
     * @throws NotFoundException If the book with the given ID does not exist.
     */
    public BookDTO updateBook(Long id, @Valid BookRequest bookRequest) {
        if (bookRepo.existsById(id)) {
            // Note: In a real application, you would fetch the entity, update its fields from the DTO,
            // and then save. The current implementation creates a new entity from the request,
            // which might lose existing fields not present in the DTO.
            Book book = bookConverter.toEntity(bookRequest);
            book.setId(id); // Ensure the ID is set on the entity for update
            Book savedBook = bookRepo.save(book);
            return bookConverter.toDto(savedBook);
        }
        throw new NotFoundException("Book with ID " + id + " not found!");
    }

    /**
     * Deletes a book identified by its ID.
     *
     * @param id The ID of the book to delete.
     * @throws NotFoundException If the book with the given ID does not exist.
     */
    public void deleteBook(Long id) {
        if (bookRepo.existsById(id)) {
            bookRepo.deleteById(id);
            return;
        }
        throw new NotFoundException("Book with ID " + id + " not found!");
    }
}