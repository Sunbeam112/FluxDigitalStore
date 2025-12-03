package com.artemhontar.fluxdigitalstore.service;

import com.artemhontar.fluxdigitalstore.api.model.Book.BookDTO;
import com.artemhontar.fluxdigitalstore.api.model.Book.BookFilter;
import com.artemhontar.fluxdigitalstore.api.model.Book.BookRequest;
import com.artemhontar.fluxdigitalstore.exception.BookAlreadyExistsException;
import com.artemhontar.fluxdigitalstore.exception.NotFoundException;
import com.artemhontar.fluxdigitalstore.model.Book;
import com.artemhontar.fluxdigitalstore.model.repo.BookRepo;
import com.artemhontar.fluxdigitalstore.service.Books.BookConverter;
import com.artemhontar.fluxdigitalstore.service.Books.BookService;
import com.artemhontar.fluxdigitalstore.service.Books.ISBNUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepo bookRepo;
    @Mock
    private BookConverter bookConverter;
    @Mock
    private ISBNUtil iSBNUtil;

    @InjectMocks
    private BookService bookService;

    // --- Mock Data ---
    private final Long TEST_ID = 1L;
    private final String TEST_ISBN_RAW = "9781234567897";
    private final String TEST_ISBN_NORMALIZED = "978-1-23-456789-7";
    private Book mockBook;
    private BookDTO mockBookDTO;
    private BookRequest mockBookRequest;
    private Pageable mockPageable;

    @BeforeEach
    void setUp() {
        // --- 1. Mock Data Setup (Essential for Service Tests) ---
        mockBook = new Book();
        mockBook.setId(TEST_ID);
        mockBook.setIsbn(TEST_ISBN_NORMALIZED);
        mockBook.setTitle("Mock Book Title");
        mockBook.setPrice(new BigDecimal("19.99"));

        // FIX: Use BookDTO.builder() instead of direct constructor call
        mockBookDTO = BookDTO.builder()
                .id(TEST_ID)
                .title("Mock Book Title")
                .authorNames(Set.of("Mock Author"))
                .isbn(TEST_ISBN_NORMALIZED)
                .price(new BigDecimal("19.99"))
                .publicationYear(2020)
                .shortDescription("Mock Short Desc") // Added new DTO field
                .category("Category X")
                .subcategory("Subcategory Y") // Added new DTO field
                .categoryNames(Collections.emptySet())
                .urlPhoto("http://mock.url/photo.jpg") // Added new DTO field
                .build();

        // FIX: Use BookRequest.builder() instead of direct constructor call
        // Ensure all @NotNull / @NotBlank / @NotEmpty fields are set
        mockBookRequest = BookRequest.builder()
                .title("Mock Book Title")
                .authorNames(Set.of("Mock Author"))
                .isbn(TEST_ISBN_RAW)
                .price(new BigDecimal("19.99"))
                .publicationYear(2020)
                .description("Mock detailed description.") // @NotBlank
                .shortDescription("Mock Short Desc")
                .category("Category X") // @NotBlank
                .subcategory("Subcategory Y")
                .categoryIds(Collections.emptySet())
                .urlPhoto("http://mock.url/photo.jpg")
                .build();

        mockPageable = PageRequest.of(0, 10);

        // --- 2. Mock Converter Behavior (Essential for Service Tests) ---
        // These stubs are needed for various tests that convert between DTO/Entity/Request
        lenient().when(bookConverter.toDto(any(Book.class))).thenReturn(mockBookDTO);
        lenient().when(bookConverter.toEntity(eq(mockBookRequest))).thenReturn(mockBook);
        lenient().when(bookConverter.normalizeISBN(eq(TEST_ISBN_RAW))).thenReturn(TEST_ISBN_NORMALIZED);
    }

    // The rest of the test methods remain unchanged as the issue was only in setUp
    // ===================================
    // TEST: getBookByIsbn
    // ===================================

    @Test
    void getBookByIsbn_ValidISBN_ReturnsBookDTO() {
        // Arrange
        when(iSBNUtil.isValidISBN(TEST_ISBN_RAW)).thenReturn(true);
        when(bookRepo.findByIsbnIgnoreCase(TEST_ISBN_NORMALIZED)).thenReturn(mockBook);

        // Act
        Optional<BookDTO> result = bookService.getBookByIsbn(TEST_ISBN_RAW);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_ISBN_NORMALIZED, result.get().getIsbn());
        verify(iSBNUtil).isValidISBN(TEST_ISBN_RAW);
        // The service implementation should call normalizeISBN before accessing the repo.
        verify(bookConverter).normalizeISBN(TEST_ISBN_RAW);
        verify(bookRepo).findByIsbnIgnoreCase(TEST_ISBN_NORMALIZED);
    }

    @Test
    void getBookByIsbn_InvalidISBN_ReturnsEmptyOptional() {
        // Arrange
        when(iSBNUtil.isValidISBN(TEST_ISBN_RAW)).thenReturn(false);

        // Act
        Optional<BookDTO> result = bookService.getBookByIsbn(TEST_ISBN_RAW);

        // Assert
        assertTrue(result.isEmpty());
        verify(iSBNUtil).isValidISBN(TEST_ISBN_RAW);
        verify(bookRepo, never()).findByIsbnIgnoreCase(anyString());
        verify(bookConverter, never()).normalizeISBN(anyString()); // Should not attempt normalization
    }

    @Test
    void getBookByIsbn_ValidISBNNotFound_ReturnsEmptyOptional() {
        // Arrange
        when(iSBNUtil.isValidISBN(TEST_ISBN_RAW)).thenReturn(true);
        when(bookRepo.findByIsbnIgnoreCase(TEST_ISBN_NORMALIZED)).thenReturn(null);

        // Act
        Optional<BookDTO> result = bookService.getBookByIsbn(TEST_ISBN_RAW);

        // Assert
        assertTrue(result.isEmpty());
        verify(bookRepo).findByIsbnIgnoreCase(TEST_ISBN_NORMALIZED);
    }

    // ===================================
    // TEST: getBookByID
    // ===================================

    @Test
    void getBookByID_Found_ReturnsBookDTO() {
        // Arrange
        when(bookRepo.findById(TEST_ID)).thenReturn(Optional.of(mockBook));

        // Act
        Optional<BookDTO> result = bookService.getBookByID(TEST_ID);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_ID, result.get().getId());
        verify(bookConverter).toDto(mockBook);
    }

    @Test
    void getBookByID_NotFound_ReturnsEmptyOptional() {
        // Arrange
        when(bookRepo.findById(TEST_ID)).thenReturn(Optional.empty());

        // Act
        Optional<BookDTO> result = bookService.getBookByID(TEST_ID);

        // Assert
        assertTrue(result.isEmpty());
        verify(bookConverter, never()).toDto(any());
    }

    // ===================================
    // TEST: getAllBooksPageable
    // ===================================

    @Test
    void getAllBooksPageable_ReturnsPagedDTOs() {
        // Arrange
        Page<Book> mockPage = new PageImpl<>(List.of(mockBook));
        when(bookRepo.findAll(mockPageable)).thenReturn(mockPage);

        // Act
        Page<BookDTO> resultPage = bookService.getAllBooksPageable(mockPageable);

        // Assert
        assertEquals(1, resultPage.getTotalElements());
        assertEquals(mockBookDTO.getTitle(), resultPage.getContent().get(0).getTitle());
        verify(bookRepo).findAll(mockPageable);
        // Verify that the converter was used to transform the entity to DTO
        verify(bookConverter, times(1)).toDto(mockBook);
    }

    // ===================================
    // TEST: searchBooksByFilter
    // ===================================

    @Test
    void searchBooksByFilter_AppliesSpecification_ReturnsPagedDTOs() {
        // Arrange
        // Using the recommended no-argument constructor and setter
        BookFilter filter = new BookFilter();
        filter.setTitle("Author A");

        // Set up mock data and pagination
        Page<Book> mockPage = new PageImpl<>(List.of(mockBook));

        // Mocking findAll with Specification:
        when(bookRepo.findAll(any(Specification.class), eq(mockPageable))).thenReturn(mockPage);

        // Act
        Page<BookDTO> resultPage = bookService.searchBooksByFilter(filter, mockPageable);

        // Assert
        assertEquals(1, resultPage.getTotalElements());
        assertEquals(mockBookDTO.getTitle(), resultPage.getContent().get(0).getTitle());

        // Verify that the repository method was called with any valid Specification and the correct Pageable.
        verify(bookRepo).findAll(any(Specification.class), eq(mockPageable));
        verify(bookConverter, times(1)).toDto(mockBook);
    }


    // ===================================
    // TEST: createBook (Book Entity)
    // ===================================

    @Test
    void createBook_Entity_Success() {
        // Arrange
        mockBook.setIsbn(TEST_ISBN_RAW); // Ensure input has the raw ISBN
        when(bookRepo.existsByIsbnIgnoreCase(TEST_ISBN_NORMALIZED)).thenReturn(false);
        when(bookRepo.save(mockBook)).thenReturn(mockBook);

        // Act
        Book result = bookService.createBook(mockBook);

        // Assert
        assertEquals(TEST_ISBN_NORMALIZED, result.getIsbn(), "ISBN must be normalized before saving.");
        verify(bookConverter).normalizeISBN(TEST_ISBN_RAW); // Verify normalization is called
        verify(bookRepo).existsByIsbnIgnoreCase(TEST_ISBN_NORMALIZED);
        verify(bookRepo).save(mockBook);
    }

    @Test
    void createBook_Entity_ThrowsBookAlreadyExists() {
        // Arrange
        mockBook.setIsbn(TEST_ISBN_RAW);
        when(bookRepo.existsByIsbnIgnoreCase(TEST_ISBN_NORMALIZED)).thenReturn(true);

        // Act & Assert
        assertThrows(BookAlreadyExistsException.class, () -> bookService.createBook(mockBook));
        verify(bookRepo, never()).save(any(Book.class));
    }

    // ===================================
    // TEST: createBook (BookRequest DTO)
    // ===================================

    @Test
    void createBook_Request_Success() {
        // Arrange
        when(bookRepo.existsByIsbnIgnoreCase(TEST_ISBN_NORMALIZED)).thenReturn(false);
        when(bookRepo.save(mockBook)).thenReturn(mockBook);

        // Act
        BookDTO result = bookService.createBook(mockBookRequest);

        // Assert
        assertEquals(mockBookDTO.getTitle(), result.getTitle());
        verify(bookConverter).toEntity(mockBookRequest);
        verify(bookRepo).save(mockBook);
        verify(bookConverter).toDto(mockBook);
    }

    // ===================================
    // TEST: createAllBooksFromList
    // ===================================

    @Test
    void createAllBooksFromList_Success() {
        // Arrange
        List<Book> booksToSave = List.of(mockBook);
        when(bookRepo.saveAll(booksToSave)).thenReturn(booksToSave);

        // Act
        Iterable<BookDTO> results = bookService.createAllBooksFromList(booksToSave);

        // Assert
        assertNotNull(results);
        assertEquals(1, ((List<?>) results).size());
        verify(bookRepo).saveAll(booksToSave);
        verify(bookConverter, times(1)).toDto(mockBook); // Verify conversion of the result
    }

    @Test
    void createAllBooksFromList_NullList_ThrowsIllegalArgumentException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> bookService.createAllBooksFromList(null));
        verify(bookRepo, never()).saveAll(any());
    }

    @Test
    void createAllBooksFromList_EmptyList_ThrowsIllegalArgumentException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> bookService.createAllBooksFromList(Collections.emptyList()));
        verify(bookRepo, never()).saveAll(any());
    }

    // ===================================
    // TEST: updateBook
    // ===================================

    @Test
    void updateBook_Success() {
        // Arrange
        when(bookRepo.existsById(TEST_ID)).thenReturn(true);
        // Ensure the converter is mocked to return the entity for saving
        when(bookConverter.toEntity(mockBookRequest)).thenReturn(mockBook);

        // Use doAnswer to simulate the repository setting the ID on the entity before saving
        doAnswer(invocation -> {
            Book entity = invocation.getArgument(0);
            entity.setId(TEST_ID); // Set ID for assertion
            return entity;
        }).when(bookRepo).save(any(Book.class));

        // Act
        BookDTO result = bookService.updateBook(TEST_ID, mockBookRequest);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_ID, result.getId());
        verify(bookRepo).existsById(TEST_ID);
        // Verify that the entity created from the request has the ID set and is saved
        verify(bookConverter).toEntity(mockBookRequest);
        verify(bookRepo).save(mockBook);
        verify(bookConverter).toDto(mockBook);
    }

    @Test
    void updateBook_NotFound_ThrowsNotFoundException() {
        // Arrange
        when(bookRepo.existsById(TEST_ID)).thenReturn(false);

        // Act & Assert
        assertThrows(NotFoundException.class, () -> bookService.updateBook(TEST_ID, mockBookRequest));
        verify(bookRepo, never()).save(any(Book.class));
    }

    // ===================================
    // TEST: deleteBook
    // ===================================

    @Test
    void deleteBook_Success() {
        // Arrange
        when(bookRepo.existsById(TEST_ID)).thenReturn(true);
        doNothing().when(bookRepo).deleteById(TEST_ID);

        // Act & Assert (No exception expected)
        assertDoesNotThrow(() -> bookService.deleteBook(TEST_ID));
        verify(bookRepo).existsById(TEST_ID);
        verify(bookRepo).deleteById(TEST_ID);
    }

    @Test
    void deleteBook_NotFound_ThrowsNotFoundException() {
        // Arrange
        when(bookRepo.existsById(TEST_ID)).thenReturn(false);

        // Act & Assert
        assertThrows(NotFoundException.class, () -> bookService.deleteBook(TEST_ID));
        verify(bookRepo, never()).deleteById(anyLong());
    }

    // ===================================
    // TEST: existsByID
    // ===================================

    @Test
    void existsByID_ReturnsRepoResult() {
        // Arrange
        when(bookRepo.existsById(TEST_ID)).thenReturn(true);

        // Act
        boolean exists = bookService.existsByID(TEST_ID);

        // Assert
        assertTrue(exists);
        verify(bookRepo).existsById(TEST_ID);
    }
}