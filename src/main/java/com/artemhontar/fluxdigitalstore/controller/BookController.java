package com.artemhontar.fluxdigitalstore.controller;

import com.artemhontar.fluxdigitalstore.api.model.Book.BookDTO;
import com.artemhontar.fluxdigitalstore.api.model.Book.BookFilter;
import com.artemhontar.fluxdigitalstore.api.model.Book.BookRequest;
import com.artemhontar.fluxdigitalstore.exception.BookAlreadyExistsException;
import com.artemhontar.fluxdigitalstore.exception.NotFoundException;
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
    public ResponseEntity<Page<BookDTO>> getAllBooks(
            @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {

        Page<BookDTO> booksPage = bookService.getAllBooksPageable(pageable);

        if (booksPage.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(booksPage, HttpStatus.OK);
    }

    @GetMapping("/search_by_id")
    public ResponseEntity<BookDTO> getBookById(@RequestParam Long id) {
        try {
            Optional<BookDTO> opBook = bookService.getBookByID(id);
            return opBook.map(book -> new ResponseEntity<>(book, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/search")
    public ResponseEntity<Page<BookDTO>> combinedBookSearch(
            BookFilter filter,
            @PageableDefault(size = 10, page = 0, sort = "title") Pageable pageable) {

        Page<BookDTO> booksPage = bookService.searchBooksByFilter(filter, pageable);

        if (booksPage.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(booksPage, HttpStatus.OK);
    }

    @PostMapping("/create")
    public ResponseEntity<BookDTO> createBook(@Valid @RequestBody BookRequest bookRequest) {
        try {
            BookDTO response = bookService.createBook(bookRequest);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (BookAlreadyExistsException e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PutMapping("/update")
    public ResponseEntity<BookDTO> updateBook(
            @RequestParam Long id,
            @Valid @RequestBody BookRequest bookRequest) {
        try {
            BookDTO updatedBook = bookService.updateBook(id, bookRequest);
            return new ResponseEntity<>(updatedBook, HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteBook(@RequestParam Long id) {
        try {
            bookService.deleteBook(id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}