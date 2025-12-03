package com.artemhontar.fluxdigitalstore.service;

import com.artemhontar.fluxdigitalstore.api.model.Order.DeliveryAddressDTO;
import com.artemhontar.fluxdigitalstore.api.model.Order.OrderItemDTO;
import com.artemhontar.fluxdigitalstore.api.model.User.UserOrderRequest;
import com.artemhontar.fluxdigitalstore.exception.*;
import com.artemhontar.fluxdigitalstore.model.*;
import com.artemhontar.fluxdigitalstore.model.enums.ORDER_STATUS;
import com.artemhontar.fluxdigitalstore.model.repo.BookRepo; // NEW MOCK
import com.artemhontar.fluxdigitalstore.model.repo.OrderRepo;
import com.artemhontar.fluxdigitalstore.service.Books.BookService;
import com.artemhontar.fluxdigitalstore.service.Order.*;
import com.artemhontar.fluxdigitalstore.service.User.AuthenticationService;
import com.artemhontar.fluxdigitalstore.service.User.DeliveryAddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    // --- Services/Components under test or mocked dependencies ---
    @Mock private FinanceService financeService;
    @Mock private AuthenticationService authenticationService;
    @Mock private OrderRepo orderRepo;
    @Mock private InventoryService inventoryService;
    @Mock private OrderConverter orderConverter; // CONVERTER MOCK
    @Mock private DeliveryAddressService deliveryAddressService;
    @Mock private BookService bookService;

    // NEW MOCK for OrderConverter's internal dependency
    @Mock private BookRepo bookRepo;

    @InjectMocks
    private OrderService orderService; // OrderService under test

    @InjectMocks
    private OrderConverter actualOrderConverter; // Injecting into the real converter for unit testing its logic

    // --- Mock Data ---
    private LocalUser mockUser;
    private LocalUser mockAdminUser;
    private UserOrderRequest mockRequest;
    private OrderItemDTO mockOrderItemDTO;
    private OrderItem mockOrderItemEntity;
    private UserOrder mockOrder;
    private DeliveryAddress mockAddress;
    private DeliveryAddressDTO mockAddressDTO;

    private final Long TEST_USER_ID = 1L;
    private final Long TEST_ADMIN_ID = 2L;
    private final Long TEST_ORDER_ID = 10L;
    private final Long TEST_PRODUCT_ID = 100L;
    private final int TEST_QUANTITY = 5;
    private final String MOCK_PAYMENT_ID = "test-payment-123";

    @BeforeEach
    void setUp() throws NotFoundException {
        // Mock User
        mockUser = new LocalUser();
        mockUser.setId(TEST_USER_ID);
        mockUser.setEmail("user@mail.com");

        // Mock Admin User
        mockAdminUser = new LocalUser();
        mockAdminUser.setId(TEST_ADMIN_ID);
        mockAdminUser.setEmail("admin@mail.com");

        // Mock OrderItem DTO and Entity
        mockOrderItemDTO = OrderItemDTO.builder()
                .productId(TEST_PRODUCT_ID)
                .quantity(TEST_QUANTITY)
                .build();
        mockOrderItemEntity = new OrderItem();
        mockOrderItemEntity.setProductId(TEST_PRODUCT_ID);
        mockOrderItemEntity.setQuantity(TEST_QUANTITY);
        mockOrderItemEntity.setUserOrder(mockOrder);

        // Mock Delivery Address DTO and Entity
        mockAddressDTO = new DeliveryAddressDTO();
        mockAddressDTO.setAddressLine1("123 Test St");
        mockAddress = new DeliveryAddress();
        mockAddress.setId(5L);
        mockAddress.setLocalUser(mockUser);

        // Mock UserOrderRequest
        mockRequest = new UserOrderRequest();
        mockRequest.setOrderItems(List.of(mockOrderItemDTO));
        mockRequest.setDeliveryAddress(mockAddressDTO);

        // Mock UserOrder
        mockOrder = new UserOrder();
        mockOrder.setId(TEST_ORDER_ID);
        mockOrder.setLocalUser(mockUser);
        mockOrder.setOrderItems(List.of(mockOrderItemEntity));
        mockOrder.setStatus(ORDER_STATUS.PROCESSING);
        mockOrder.setDeliveryAddress(mockAddress);

        // CRITICAL Mocks for Order Generation
        // When the *MOCK* converter is used by orderService
        lenient().when(orderConverter.convertToEntity(mockOrderItemDTO)).thenReturn(mockOrderItemEntity);

        // Mock successful address retrieval
        lenient().when(deliveryAddressService.getDeliveryAddress(mockRequest, mockUser)).thenReturn(Optional.of(mockAddress));

        // Mock book existence for validation
        lenient().when(bookService.existsByID(TEST_PRODUCT_ID)).thenReturn(true);
    }

    // ===================================
    // TEST: OrderService.createOrder (Full flow)
    // ===================================

    @Test
    void createOrder_Success_ReturnsProcessingOrder() throws Exception {
        // Arrange
        when(financeService.checkout(any(UserOrderRequest.class))).thenReturn(MOCK_PAYMENT_ID);
        when(authenticationService.tryGetCurrentUser()).thenReturn(Optional.of(mockUser));

        // Mock two saves: first to get ID, second to set PROCESSING status
        when(orderRepo.save(any(UserOrder.class)))
                .thenAnswer(invocation -> {
                    UserOrder savedOrder = invocation.getArgument(0);
                    if (savedOrder.getId() == null) {
                        savedOrder.setId(TEST_ORDER_ID);
                    }
                    return savedOrder;
                });

        ArgumentCaptor<UserOrder> orderCaptor = ArgumentCaptor.forClass(UserOrder.class);

        // Act
        UserOrder resultOrder = orderService.createOrder(mockRequest);

        // Assert
        assertEquals(ORDER_STATUS.PROCESSING, resultOrder.getStatus());
        verify(orderRepo, times(2)).save(orderCaptor.capture());

        // Verify Inventory Reservation
        verify(inventoryService, times(1)).reserveStock(
                TEST_PRODUCT_ID, TEST_QUANTITY, TEST_ORDER_ID);
    }

    @Test
    void createOrder_StockFails_OrderIsCancelledAndThrowsStockException() throws Exception {
        // Arrange
        when(financeService.checkout(any())).thenReturn(MOCK_PAYMENT_ID);
        when(authenticationService.tryGetCurrentUser()).thenReturn(Optional.of(mockUser));

        // Mock first save
        when(orderRepo.save(any(UserOrder.class))).thenAnswer(invocation -> {
            UserOrder savedOrder = invocation.getArgument(0);
            savedOrder.setId(TEST_ORDER_ID);
            return savedOrder;
        });

        // Mock Stock Failure after first save
        doThrow(new NotEnoughStock("Out of stock"))
                .when(inventoryService).reserveStock(anyLong(), anyInt(), anyLong());

        // Act & Assert
        assertThrows(NotEnoughStock.class, () -> orderService.createOrder(mockRequest));

        // Verify status was set to CANCELLED and saved
        ArgumentCaptor<UserOrder> orderCaptor = ArgumentCaptor.forClass(UserOrder.class);

        // Should be 2 saves: 1st PENDING_RESERVATION, 2nd CANCELLED
        verify(orderRepo, times(2)).save(orderCaptor.capture());

        UserOrder finalSavedOrder = orderCaptor.getAllValues().get(1);
        assertEquals(ORDER_STATUS.CANCELLED, finalSavedOrder.getStatus());
    }

    // ===================================
    // TEST: OrderConverter.convertToEntity (Unit Test)
    // ===================================

    @Test
    void convertToEntity_ProductNotFound_ThrowsNotFoundException() {
        // Arrange
        // Mock the actual dependency used by the real converter: BookRepo
        when(bookRepo.findById(TEST_PRODUCT_ID)).thenReturn(Optional.empty());

        // Act & Assert - Use the injected 'actualOrderConverter'
        assertThrows(NotFoundException.class, () -> actualOrderConverter.convertToEntity(mockOrderItemDTO));
    }

    @Test
    void convertToEntity_Success_ReturnsOrderItemEntity() {
        // Arrange
        Book mockBook = new Book();
        mockBook.setId(TEST_PRODUCT_ID);
        when(bookRepo.findById(TEST_PRODUCT_ID)).thenReturn(Optional.of(mockBook));

        // Act - Use the injected 'actualOrderConverter'
        OrderItem resultItem = actualOrderConverter.convertToEntity(mockOrderItemDTO);

        // Assert
        assertNotNull(resultItem);
        assertEquals(TEST_PRODUCT_ID, resultItem.getProductId());
        assertEquals(TEST_QUANTITY, resultItem.getQuantity());
        assertEquals(mockBook, resultItem.getBook());
        assertFalse(resultItem.isDispatched());
    }

    // ===================================
    // TEST: DeliveryAddressService.getDeliveryAddress (Integrated Test)
    // ===================================

    @Test
    void createOrder_UsesRequestedAddressId_Success() throws Exception {
        // Arrange
        Long requestedAddressId = 99L;
        mockRequest.getDeliveryAddress().setId(requestedAddressId);

        DeliveryAddress specificAddress = new DeliveryAddress();
        specificAddress.setId(requestedAddressId);

        // Mock the address service to return the specific requested address
        when(deliveryAddressService.getDeliveryAddress(mockRequest, mockUser)).thenReturn(Optional.of(specificAddress));

        when(financeService.checkout(any())).thenReturn(MOCK_PAYMENT_ID);
        when(authenticationService.tryGetCurrentUser()).thenReturn(Optional.of(mockUser));
        when(orderRepo.save(any(UserOrder.class))).thenAnswer(invocation -> {
            UserOrder savedOrder = invocation.getArgument(0);
            savedOrder.setId(TEST_ORDER_ID);
            return savedOrder;
        });

        ArgumentCaptor<UserOrder> orderCaptor = ArgumentCaptor.forClass(UserOrder.class);

        // Act
        orderService.createOrder(mockRequest);

        // Assert
        verify(orderRepo, times(2)).save(orderCaptor.capture());
        UserOrder initialSave = orderCaptor.getAllValues().get(0);

        // Verify the initial order was created with the *requested* specific address
        assertNotNull(initialSave.getDeliveryAddress());
        assertEquals(requestedAddressId, initialSave.getDeliveryAddress().getId());
    }

    @Test
    void createOrder_FallsBackToUserAddress_Success() throws Exception {
        // Arrange
        // 1. Clear ID from DTO (simulating address by value or no specific ID)
        mockRequest.getDeliveryAddress().setId(null);

        // 2. Mock address service to return the user's first saved address (fallback)
        DeliveryAddress fallbackAddress = new DeliveryAddress();
        fallbackAddress.setId(50L);
        when(deliveryAddressService.getDeliveryAddress(mockRequest, mockUser)).thenReturn(Optional.of(fallbackAddress));

        when(financeService.checkout(any())).thenReturn(MOCK_PAYMENT_ID);
        when(authenticationService.tryGetCurrentUser()).thenReturn(Optional.of(mockUser));
        when(orderRepo.save(any(UserOrder.class))).thenAnswer(invocation -> {
            UserOrder savedOrder = invocation.getArgument(0);
            savedOrder.setId(TEST_ORDER_ID);
            return savedOrder;
        });

        ArgumentCaptor<UserOrder> orderCaptor = ArgumentCaptor.forClass(UserOrder.class);

        // Act
        orderService.createOrder(mockRequest);

        // Assert
        verify(orderRepo, times(2)).save(orderCaptor.capture());
        UserOrder initialSave = orderCaptor.getAllValues().get(0);

        // Verify the initial order was created with the *fallback* specific address
        assertNotNull(initialSave.getDeliveryAddress());
        assertEquals(50L, initialSave.getDeliveryAddress().getId());
    }

    // ===================================
    // TEST: OrderService.dispatchOrder (Authorization)
    // ===================================

    @Test
    void dispatchOrder_SuccessByOwner_ReturnsDispatchedOrder() {
        // Arrange
        mockOrder.setStatus(ORDER_STATUS.PROCESSING);
        mockOrder.getDeliveryAddress().setId(mockAddress.getId());

        when(orderRepo.findById(TEST_ORDER_ID)).thenReturn(Optional.of(mockOrder));
        when(authenticationService.isAdmin()).thenReturn(false);
        when(authenticationService.tryGetCurrentUser()).thenReturn(Optional.of(mockUser)); // Current user is owner
        when(orderRepo.save(any(UserOrder.class))).thenReturn(mockOrder);

        // Act
        UserOrder dispatchedOrder = orderService.dispatchOrder(TEST_ORDER_ID);

        // Assert
        assertEquals(ORDER_STATUS.DISPATCHED, dispatchedOrder.getStatus());
        verify(inventoryService, times(1)).dispatchStock(anyLong(), anyInt(), anyLong(), anyLong());
    }

    @Test
    void dispatchOrder_NotOwnerOrAdmin_ThrowsUnauthorizedException() {
        // Arrange
        LocalUser strangerUser = new LocalUser();
        strangerUser.setId(99L);
        mockOrder.setStatus(ORDER_STATUS.PROCESSING);

        when(orderRepo.findById(TEST_ORDER_ID)).thenReturn(Optional.of(mockOrder));
        when(authenticationService.isAdmin()).thenReturn(false);
        when(authenticationService.tryGetCurrentUser()).thenReturn(Optional.of(strangerUser)); // Current user is not owner

        // Act & Assert
        assertThrows(UnauthorizedAccessException.class, () -> orderService.dispatchOrder(TEST_ORDER_ID));

        verify(inventoryService, never()).dispatchStock(anyLong(), anyInt(), anyLong(), anyLong());
    }

    // ===================================
    // TEST: OrderService.validateOrderRequest
    // ===================================

    @Test
    void validateOrderRequest_ProductDoesNotExist_ThrowsIllegalArgumentException() {
        // Arrange
        when(bookService.existsByID(TEST_PRODUCT_ID)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> orderService.validateOrderRequest(mockRequest));
    }
}