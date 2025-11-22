package com.artemhontar.fluxdigitalstore.controller;

import com.artemhontar.fluxdigitalstore.api.model.Book.BookFilter;
import com.artemhontar.fluxdigitalstore.api.model.Book.BookRequest;
import com.artemhontar.fluxdigitalstore.exception.BookAlreadyExistsException;
import com.artemhontar.fluxdigitalstore.model.Book;
import com.artemhontar.fluxdigitalstore.service.Books.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Validated
@RestController()
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/get_all")
    public ResponseEntity<Page<Book>> getAllBooks(

            @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {

        Page<Book> booksPage = bookService.getAllBooksPageable(pageable);

        if (booksPage.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(booksPage, HttpStatus.OK);
    }

    @GetMapping("/search_by_id")
    public ResponseEntity<Book> getBookById(@RequestParam Long id) {
        try {
            Optional<Book> opBook = bookService.getBookByID(id);
            return opBook.map(book -> new ResponseEntity<>(book, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Book> createBook(@Valid @RequestBody BookRequest bookRequest) {
        try {
            Book response = bookService.createBook(bookRequest);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (BookAlreadyExistsException e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/search")
    public ResponseEntity<Page<Book>> combinedBookSearch(
            BookFilter filter,
            @PageableDefault(size = 10, page = 0, sort = "title") Pageable pageable) {

        Page<Book> booksPage = bookService.searchBooksByFilter(filter, pageable);

        if (booksPage.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(booksPage, HttpStatus.OK);
    }
}