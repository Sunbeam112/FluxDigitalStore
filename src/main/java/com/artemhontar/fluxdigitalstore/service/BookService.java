package com.artemhontar.fluxdigitalstore.service;

import com.artemhontar.fluxdigitalstore.api.model.BookRequest;
import com.artemhontar.fluxdigitalstore.exception.BookAlreadyExistsException;
import com.artemhontar.fluxdigitalstore.model.Book;
import com.artemhontar.fluxdigitalstore.repo.BookRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class BookService {


    private final BookRepo bookRepo;
    private final BookConverter bookConverter;
    private final ISBNUtil iSBNUtil;

    public BookService(BookRepo bookRepo, BookConverter bookConverter, ISBNUtil iSBNUtil) {
        this.bookRepo = bookRepo;
        this.bookConverter = bookConverter;
        this.iSBNUtil = iSBNUtil;
    }

    public Optional<Book> getBookByIsbn(String isbn) {
        boolean isInputIsISBN = iSBNUtil.isValidISBN(isbn);
        if (isInputIsISBN) {
            String normalizedISBN = bookConverter.normalizeISBN(isbn);
            if (bookRepo.existsByIsbnIgnoreCase(normalizedISBN)) {
                return Optional.of(bookRepo.getByIsbnIgnoreCase(normalizedISBN));
            }
        }
        return Optional.empty();
    }

    public Optional<Book> getBookByID(Long id) {
        if (bookRepo.existsById(id)) {
            return bookRepo.findById(id);
        }
        return Optional.empty();
    }

    public Page<Book> getAllBooksByTitle(String title, Pageable pageable) {
        return bookRepo.getByTitleIgnoreCaseAllIgnoreCase(title, pageable);
    }

    public Page<Book> getAllBooksByAuthor(String author, Pageable pageable) {
        return bookRepo.findByAuthorContainingCaseInsensitive(author, pageable);
    }


    public Page<Book> getAllBooksPageable(Pageable pageable) {
        return bookRepo.findAll(pageable);
    }

    public Book createBook(Book book) {
        if (bookRepo.existsByIsbnIgnoreCase(book.getIsbn())) {
            throw new BookAlreadyExistsException("The book with id ${book.getIsbn() is already exists!}");
        }
        return bookRepo.save(book);
    }


    public Book createBook(BookRequest bookRequest) {
        if (bookRepo.existsByIsbnIgnoreCase(bookRequest.getIsbn())) {
            throw new BookAlreadyExistsException("The book with id ${bookRequest.getIsbn() is already exists!}");
        }
        Book book = bookConverter.toEntity(bookRequest);
        return bookRepo.save(book);

    }


    public Page<Book> getBooksByPriceLimit(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return bookRepo.findByPriceBetween(minPrice, maxPrice, pageable);
    }
}