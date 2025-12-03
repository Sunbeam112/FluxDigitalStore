package com.artemhontar.fluxdigitalstore.controller;

import com.artemhontar.fluxdigitalstore.api.model.Book.BookDTO;
import com.artemhontar.fluxdigitalstore.api.model.Book.BookFilter;
import com.artemhontar.fluxdigitalstore.api.model.Book.BookRequest;
import com.artemhontar.fluxdigitalstore.api.security.JWTUtils;
import com.artemhontar.fluxdigitalstore.exception.BookAlreadyExistsException;
import com.artemhontar.fluxdigitalstore.exception.NotFoundException;
import com.artemhontar.fluxdigitalstore.model.repo.UserRepo;
import com.artemhontar.fluxdigitalstore.service.Books.BookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@Import(BookControllerTest.TestConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookService bookService;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private JWTUtils jwtUtils;

    private final String BASE_URL = "/books";
    private final PageRequest DEFAULT_PAGEABLE = PageRequest.of(0, 10);

    @TestConfiguration
    static class TestConfig {
        @Bean
        public BookService bookService() {
            return mock(BookService.class);
        }

        @Bean
        public UserRepo userRepo() {
            return mock(UserRepo.class);
        }

        @Bean
        public JWTUtils jwtUtils() {
            return mock(JWTUtils.class);
        }
    }

    @BeforeEach
    void setup() {
        reset(bookService, userRepo, jwtUtils);
    }

    private BookDTO createMockBookDTO(Long id) {
        return BookDTO.builder()
                .id(id)
                .title("The Hitchhiker's Guide to the Galaxy")
                .isbn("9780345391803")
                .price(new BigDecimal("12.99"))
                .authorNames(Set.of("Douglas Adams"))
                .build();
    }

    private BookRequest createMockBookRequest() {
        return BookRequest.builder()
                .title("New Book Title")
                .isbn("9781234567890")
                .price(new BigDecimal("20.00"))
                .publicationYear(2023)
                .description("Long description.")
                .shortDescription("Short desc.")
                .category("Sci-Fi")
                .authorNames(Set.of("Author A"))
                .build();
    }

    // --- GET /get_all Tests ---

    @Test
    void getAllBooks_Returns200OkWithContent() throws Exception {
        List<BookDTO> books = List.of(createMockBookDTO(1L), createMockBookDTO(2L));
        Page<BookDTO> page = new PageImpl<>(books, DEFAULT_PAGEABLE, 2);

        when(bookService.getAllBooksPageable(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(BASE_URL + "/get_all")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1L));

        verify(bookService, times(1)).getAllBooksPageable(any(Pageable.class));
    }

    @Test
    void getAllBooks_Returns204NoContent_WhenEmpty() throws Exception {
        Page<BookDTO> emptyPage = new PageImpl<>(Collections.emptyList(), DEFAULT_PAGEABLE, 0);

        when(bookService.getAllBooksPageable(any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get(BASE_URL + "/get_all"))
                .andExpect(status().isNoContent());
    }

    // --- GET /search_by_id Tests ---

    @Test
    void getBookById_Returns200OkWithBook() throws Exception {
        Long bookId = 1L;
        BookDTO mockBook = createMockBookDTO(bookId);

        when(bookService.getBookByID(bookId)).thenReturn(Optional.of(mockBook));

        mockMvc.perform(get(BASE_URL + "/search_by_id")
                        .param("id", bookId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookId))
                .andExpect(jsonPath("$.title").value("The Hitchhiker's Guide to the Galaxy"));
    }

    @Test
    void getBookById_Returns204NoContent_WhenBookNotFound() throws Exception {
        Long bookId = 99L;

        when(bookService.getBookByID(bookId)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL + "/search_by_id")
                        .param("id", bookId.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    void getBookById_Returns500InternalServerError_OnServiceException() throws Exception {
        Long bookId = 1L;

        when(bookService.getBookByID(bookId)).thenThrow(new RuntimeException("DB Connection Error"));

        mockMvc.perform(get(BASE_URL + "/search_by_id")
                        .param("id", bookId.toString()))
                .andExpect(status().isInternalServerError());
    }

    // --- GET /search Tests (combinedBookSearch) ---

    @Test
    void combinedBookSearch_Returns200OkWithContent() throws Exception {
        List<BookDTO> books = List.of(createMockBookDTO(3L));
        Page<BookDTO> page = new PageImpl<>(books, DEFAULT_PAGEABLE, 1);

        when(bookService.searchBooksByFilter(any(BookFilter.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(BASE_URL + "/search")
                        .param("title", "Hitchhiker")
                        .param("minPrice", "10.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(bookService, times(1)).searchBooksByFilter(any(BookFilter.class), any(Pageable.class));
    }

    @Test
    void combinedBookSearch_Returns204NoContent_WhenEmpty() throws Exception {
        Page<BookDTO> emptyPage = new PageImpl<>(Collections.emptyList(), DEFAULT_PAGEABLE, 0);

        when(bookService.searchBooksByFilter(any(BookFilter.class), any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get(BASE_URL + "/search"))
                .andExpect(status().isNoContent());
    }

    // --- POST /create Tests ---

    @Test
    void createBook_Returns201Created() throws Exception {
        BookRequest request = createMockBookRequest();
        BookDTO createdBook = createMockBookDTO(1L);

        when(bookService.createBook(any(BookRequest.class))).thenReturn(createdBook);

        mockMvc.perform(post(BASE_URL + "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));

        verify(bookService, times(1)).createBook(any(BookRequest.class));
    }

    @Test
    void createBook_Returns409Conflict_WhenBookAlreadyExists() throws Exception {
        BookRequest request = createMockBookRequest();

        when(bookService.createBook(any(BookRequest.class))).thenThrow(new BookAlreadyExistsException("ISBN exists"));

        mockMvc.perform(post(BASE_URL + "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void createBook_Returns500InternalServerError_OnOtherException() throws Exception {
        BookRequest request = createMockBookRequest();

        when(bookService.createBook(any(BookRequest.class))).thenThrow(new RuntimeException("DB Error"));

        mockMvc.perform(post(BASE_URL + "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // --- PUT /update Tests ---

    @Test
    void updateBook_Returns200OkWithUpdatedBook() throws Exception {
        Long bookId = 1L;
        BookRequest request = createMockBookRequest();
        BookDTO updatedBook = createMockBookDTO(bookId);

        when(bookService.updateBook(eq(bookId), any(BookRequest.class))).thenReturn(updatedBook);

        mockMvc.perform(put(BASE_URL + "/update")
                        .param("id", bookId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("The Hitchhiker's Guide to the Galaxy"));

        verify(bookService, times(1)).updateBook(eq(bookId), any(BookRequest.class));
    }

    @Test
    void updateBook_Returns404NotFound() throws Exception {
        Long bookId = 99L;
        BookRequest request = createMockBookRequest();

        when(bookService.updateBook(eq(bookId), any(BookRequest.class))).thenThrow(new NotFoundException("Book not found"));

        mockMvc.perform(put(BASE_URL + "/update")
                        .param("id", bookId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateBook_Returns500InternalServerError_OnOtherException() throws Exception {
        Long bookId = 1L;
        BookRequest request = createMockBookRequest();

        when(bookService.updateBook(eq(bookId), any(BookRequest.class))).thenThrow(new RuntimeException("Unknown Error"));

        mockMvc.perform(put(BASE_URL + "/update")
                        .param("id", bookId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // --- DELETE /delete Tests ---

    @Test
    void deleteBook_Returns200Ok() throws Exception {
        Long bookId = 1L;

        doNothing().when(bookService).deleteBook(bookId);

        mockMvc.perform(delete(BASE_URL + "/delete")
                        .param("id", bookId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(bookService, times(1)).deleteBook(bookId);
    }

    @Test
    void deleteBook_Returns404NotFound() throws Exception {
        Long bookId = 99L;

        doThrow(new NotFoundException("Book not found")).when(bookService).deleteBook(bookId);

        mockMvc.perform(delete(BASE_URL + "/delete")
                        .param("id", bookId.toString()))
                .andExpect(status().isNotFound());
    }
}