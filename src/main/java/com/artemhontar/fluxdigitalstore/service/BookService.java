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
        if (bookRepo.existsByIsbnIgnoreCase(book.getIsbn())) {
            throw new BookAlreadyExistsException("The book with ISBN " + book.getIsbn() + " is already exists!");
        }
        return bookRepo.save(book);
    }

    @Transactional
    public Book createBook(BookRequest bookRequest) {
        if (bookRepo.existsByIsbnIgnoreCase(bookConverter.normalizeISBN(bookRequest.getIsbn()))) {
            throw new BookAlreadyExistsException("The book with ISBN " + bookRequest.getIsbn() + " is already exists!");
        }
        Book book = bookConverter.toEntity(bookRequest);
        return bookRepo.save(book);
    }

}