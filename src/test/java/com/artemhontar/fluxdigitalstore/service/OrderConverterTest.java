package com.artemhontar.fluxdigitalstore.service;

import com.artemhontar.fluxdigitalstore.api.model.Order.OrderDTO;
import com.artemhontar.fluxdigitalstore.api.model.Order.OrderItemDTO;
import com.artemhontar.fluxdigitalstore.exception.NotFoundException;
import com.artemhontar.fluxdigitalstore.model.Book;
import com.artemhontar.fluxdigitalstore.model.DeliveryAddress;
import com.artemhontar.fluxdigitalstore.model.OrderItem;
import com.artemhontar.fluxdigitalstore.model.UserOrder;
import com.artemhontar.fluxdigitalstore.model.enums.ORDER_STATUS;
import com.artemhontar.fluxdigitalstore.model.repo.BookRepo;
import com.artemhontar.fluxdigitalstore.service.Order.OrderConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderConverterTest {

    @Mock
    private BookRepo bookRepo;

    @InjectMocks
    private OrderConverter orderConverter;

    // --- Mock Data ---
    private final Long TEST_ORDER_ID = 10L;
    private final Long TEST_USER_ID = 1L;
    private final Long TEST_ADDRESS_ID = 5L;
    private final Long TEST_ITEM_ID = 20L;
    private final Long TEST_PRODUCT_ID = 100L;
    private final int TEST_QUANTITY = 5;
    private final Timestamp MOCK_DATE = Timestamp.from(Instant.now());
    private final Timestamp MOCK_DISPATCH_DATE = Timestamp.from(Instant.now().plusSeconds(3600));

    private UserOrder mockOrder;
    private OrderItem mockOrderItem;
    private DeliveryAddress mockDeliveryAddress;
    private Book mockBook;
    private OrderItemDTO mockOrderItemDTO;

    @BeforeEach
    void setUp() {
        // Mock Book
        mockBook = new Book();
        mockBook.setId(TEST_PRODUCT_ID);
        mockBook.setTitle("Test Book");

        // Mock OrderItem DTO (Input for Entity conversion)
        mockOrderItemDTO = OrderItemDTO.builder()
                .productId(TEST_PRODUCT_ID)
                .quantity(TEST_QUANTITY)
                .build();

        // Mock OrderItem Entity
        mockOrderItem = new OrderItem();
        mockOrderItem.setId(TEST_ITEM_ID);
        mockOrderItem.setProductId(TEST_PRODUCT_ID);
        mockOrderItem.setQuantity(TEST_QUANTITY);
        mockOrderItem.setDispatched(true);
        mockOrderItem.setDateDispatched(MOCK_DISPATCH_DATE);
        mockOrderItem.setBook(mockBook);

        // Mock Delivery Address Entity
        mockDeliveryAddress = new DeliveryAddress();
        mockDeliveryAddress.setId(TEST_ADDRESS_ID);
        mockDeliveryAddress.setFirstName("John");
        mockDeliveryAddress.setLastName("Doe");
        mockDeliveryAddress.setAddressLine1("123 Test St");
        mockDeliveryAddress.setCity("TestCity");
        mockDeliveryAddress.setZipcode("12345");
        mockDeliveryAddress.setCountry("USA");

        // Mock UserOrder Entity
        mockOrder = new UserOrder();
        mockOrder.setId(TEST_ORDER_ID);
        mockOrder.setUserId(TEST_USER_ID);
        mockOrder.setDate(MOCK_DATE);
        mockOrder.setStatus(ORDER_STATUS.DISPATCHED);
        mockOrder.setDeliveryAddress(mockDeliveryAddress);
        mockOrder.setOrderItems(List.of(mockOrderItem));
    }

    // ===================================
    // TEST: convertToDto(UserOrder)
    // ===================================

    @Test
    void convertToDto_FullOrder_Success() {
        // Act
        OrderDTO dto = orderConverter.convertToDto(mockOrder);

        // Assert primary fields
        assertNotNull(dto);
        assertEquals(TEST_ORDER_ID, dto.getId());
        assertEquals(TEST_USER_ID, dto.getUserId());
        assertEquals(ORDER_STATUS.DISPATCHED, dto.getStatus());
        assertEquals(MOCK_DATE, dto.getDate());

        // Assert Address conversion
        assertNotNull(dto.getDeliveryAddress());
        assertEquals(TEST_ADDRESS_ID, dto.getDeliveryAddress().getId());

        // Assert OrderItem list conversion and details
        assertFalse(dto.getOrderItems().isEmpty());
        OrderItemDTO itemDTO = dto.getOrderItems().get(0);
        assertEquals(TEST_ITEM_ID, itemDTO.getId());
        assertEquals(TEST_PRODUCT_ID, itemDTO.getProductId());
        assertTrue(itemDTO.isDispatched());
    }

    @Test
    void convertToDto_OrderWithNulls_HandlesGracefully() {
        // Arrange
        mockOrder.setDeliveryAddress(null);
        mockOrder.setOrderItems(null);

        // Act
        OrderDTO dto = orderConverter.convertToDto(mockOrder);

        // Assert
        assertNotNull(dto);
        assertNull(dto.getDeliveryAddress());
        assertNull(dto.getOrderItems());
    }

    @Test
    void convertToDto_OrderWithEmptyItems_ReturnsEmptyList() {
        // Arrange
        mockOrder.setOrderItems(Collections.emptyList());

        // Act
        OrderDTO dto = orderConverter.convertToDto(mockOrder);

        // Assert
        assertNotNull(dto.getOrderItems());
        assertTrue(dto.getOrderItems().isEmpty());
    }

    // ===================================
    // TEST: convertToEntity(OrderItemDTO)
    // ===================================

    @Test
    void convertToEntity_Success_ReturnsOrderItemEntity() {
        // Arrange
        when(bookRepo.findById(TEST_PRODUCT_ID)).thenReturn(Optional.of(mockBook));

        // Act
        OrderItem entity = orderConverter.convertToEntity(mockOrderItemDTO);

        // Assert
        assertNotNull(entity);
        assertEquals(TEST_PRODUCT_ID, entity.getProductId());
        assertEquals(TEST_QUANTITY, entity.getQuantity());
        assertEquals(mockBook, entity.getBook());
        assertFalse(entity.isDispatched(), "New item must be set to not dispatched");
        verify(bookRepo, times(1)).findById(TEST_PRODUCT_ID);
    }

    @Test
    void convertToEntity_ProductNotFound_ThrowsNotFoundException() {
        // Arrange
        when(bookRepo.findById(TEST_PRODUCT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException thrown = assertThrows(NotFoundException.class, () ->
                orderConverter.convertToEntity(mockOrderItemDTO)
        );

        assertEquals("Product not found with ID: " + TEST_PRODUCT_ID, thrown.getMessage());
        verify(bookRepo, times(1)).findById(TEST_PRODUCT_ID);
    }

    @Test
    void convertToEntity_NullDTO_ReturnsNull() {
        // Act
        OrderItem entity = orderConverter.convertToEntity(null);

        // Assert
        assertNull(entity);
        verify(bookRepo, never()).findById(anyLong());
    }
}