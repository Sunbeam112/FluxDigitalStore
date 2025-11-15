package com.artemhontar.fluxdigitalstore.service;

import com.artemhontar.fluxdigitalstore.api.model.BookFilter;
import com.artemhontar.fluxdigitalstore.api.model.BookRequest;
import com.artemhontar.fluxdigitalstore.api.model.BookSpecification;
import com.artemhontar.fluxdigitalstore.exception.BookAlreadyExistsException;
import com.artemhontar.fluxdigitalstore.model.Book;
import com.artemhontar.fluxdigitalstore.repo.BookRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
            return Optional.ofNullable(bookRepo.findByIsbnIgnoreCase(normalizedISBN));
        }
        return Optional.empty();
    }

    public Optional<Book> getBookByID(Long id) {
        return bookRepo.findById(id);
    }

    public Page<Book> getAllBooksPageable(Pageable pageable) {
        return bookRepo.findAll(pageable);
    }

    public Page<Book> searchBooksByFilter(BookFilter filter, Pageable pageable) {
        BookSpecification spec = new BookSpecification(filter);
        return bookRepo.findAll(spec, pageable);
    }


    @Transactional
    public Book createBook(Book book) {

        String normalizedIsbn = bookConverter.normalizeISBN(book.getIsbn());
        checkIfBookExists(normalizedIsbn);

        book.setIsbn(normalizedIsbn);
        return bookRepo.save(book);
    }

    @Transactional
    public Book createBook(BookRequest bookRequest) {
        String normalizedIsbn = bookConverter.normalizeISBN(bookRequest.getIsbn());
        checkIfBookExists(normalizedIsbn);

        Book book = bookConverter.toEntity(bookRequest);
        book.setIsbn(normalizedIsbn);
        return bookRepo.save(book);
    }

    private void checkIfBookExists(String normalizedIsbn) {
        if (bookRepo.existsByIsbnIgnoreCase(normalizedIsbn)) {
            throw new BookAlreadyExistsException("The book with ISBN " + normalizedIsbn + " already exists!");
        }
    }
}