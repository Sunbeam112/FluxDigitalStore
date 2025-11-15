package com.artemhontar.fluxdigitalstore.controller;

import com.artemhontar.fluxdigitalstore.api.model.BookRequest;
import com.artemhontar.fluxdigitalstore.exception.BookAlreadyExistsException;
import com.artemhontar.fluxdigitalstore.model.Book;
import com.artemhontar.fluxdigitalstore.service.BookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
            // All three methods now use PageableDefault for consistent default sorting
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
            if (opBook.isPresent()) {
                return new ResponseEntity<>(opBook.get(), HttpStatus.OK);
            }
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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


    @GetMapping("/search_by_title")
    public ResponseEntity<Page<Book>> getAllBooksByTitle(
            @RequestParam @NotBlank(message = "The title parameter cannot be empty or blank.") String title,

            @PageableDefault(size = 10, page = 0, sort = "title") Pageable pageable) {

        Page<Book> booksPage = bookService.getAllBooksByTitle(title, pageable);

        if (booksPage.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(booksPage, HttpStatus.OK);
    }


    @GetMapping("/search_by_author")
    public ResponseEntity<Page<Book>> getAllBooksByAuthor(
            @RequestParam @NotBlank(message = "The title parameter cannot be empty or blank.") String author,

            @PageableDefault(size = 10, page = 0, sort = "title") Pageable pageable) {

        Page<Book> booksPage = bookService.getAllBooksByAuthor(author, pageable);

        if (booksPage.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(booksPage, HttpStatus.OK);
    }

    @GetMapping("/search_by_isbn")
    public ResponseEntity<Book> getBookByISBN(@RequestParam
                                              @Size(min = 10, max = 13, message = "ISBN must be between 10 and 13 characters.")
                                              @NotBlank(message = "The ISBN cannot be empty or blank.") String isbn) {

        try {
            Optional<Book> opBook = bookService.getBookByIsbn(isbn);
            if (opBook.isPresent()) {
                return ResponseEntity.ok(opBook.get());
            }
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    // Inside your BookController.java

    @GetMapping("/price_limit")
    public ResponseEntity<Page<Book>> getBooksByPriceLimit(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 10, page = 0, sort = "title") Pageable pageable) {

        if (minPrice == null && maxPrice == null) {
            Page<Book> allBooks = bookService.getAllBooksPageable(pageable);
            return ResponseEntity.ok(allBooks);
        }


        BigDecimal actualMinPrice = (minPrice != null) ? minPrice : BigDecimal.ZERO;

        BigDecimal actualMaxPrice = (maxPrice != null) ? maxPrice : new BigDecimal("100000");

        if (actualMinPrice.compareTo(actualMaxPrice) >= 0) {
            return ResponseEntity.badRequest().build();
        }

        Page<Book> books = bookService.getBooksByPriceLimit(actualMinPrice, actualMaxPrice, pageable);

        return ResponseEntity.ok(books);
    }
}