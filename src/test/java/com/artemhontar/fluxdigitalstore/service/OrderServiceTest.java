package com.artemhontar.fluxdigitalstore.service;

import com.artemhontar.fluxdigitalstore.api.model.User.UserOrderRequest;
import com.artemhontar.fluxdigitalstore.exception.*;
import com.artemhontar.fluxdigitalstore.model.*;
import com.artemhontar.fluxdigitalstore.model.enums.ORDER_STATUS;
import com.artemhontar.fluxdigitalstore.repo.OrderRepo;
import com.artemhontar.fluxdigitalstore.service.Order.FinanceService;
import com.artemhontar.fluxdigitalstore.service.Order.InventoryService;
import com.artemhontar.fluxdigitalstore.service.Order.OrderService;
import com.artemhontar.fluxdigitalstore.service.User.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private FinanceService financeService;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private OrderRepo orderRepo;
    @Mock
    private InventoryService inventoryService;

    // Inject all mocks into the service being tested
    @InjectMocks
    private OrderService orderService;

    // --- Mock Data ---
    private LocalUser mockUser;
    private UserOrderRequest mockRequest;
    private OrderItem mockOrderItem;
    private UserOrder mockOrder;
    private DeliveryAddress mockAddress;
    private final Long TEST_USER_ID = 1L;
    private final Long TEST_ORDER_ID = 10L;
    private final String MOCK_PAYMENT_ID = "test-payment-123";

    @BeforeEach
    void setUp() {
        // Mock User
        mockUser = new LocalUser();
        mockUser.setId(TEST_USER_ID);
        mockUser.setEmail("test@mail.com");

        // Mock OrderItem
        mockOrderItem = new OrderItem();
        mockOrderItem.setProductId(100L);
        mockOrderItem.setQuantity(5);

        // Mock Delivery Address
        mockAddress = new DeliveryAddress();
        mockAddress.setId(5L);

        // Mock UserOrderRequest
        mockRequest = new UserOrderRequest();
        mockRequest.setOrderItems(List.of(mockOrderItem));

        // Mock UserOrder (setup for return values)
        mockOrder = new UserOrder();
        mockOrder.setId(TEST_ORDER_ID);
        mockOrder.setLocalUser(mockUser);
        mockOrder.setUserId(TEST_USER_ID);
        mockOrder.setOrderItems(List.of(mockOrderItem));
        mockOrder.setStatus(ORDER_STATUS.PROCESSING);
        mockOrder.setDeliveryAddress(mockAddress);
    }

    // ===================================
    // TEST: createOrder
    // ===================================

    @Test
    void createOrder_Success_ReturnsProcessingOrder() throws Exception {
        // --- FIX 1: Set up the save mock to return the object it receives ---
        // This preserves any fields (like paymentId) set by the service logic.
        when(orderRepo.save(any(UserOrder.class)))
                .thenAnswer(invocation -> {
                    // Return the UserOrder object that was passed as the first argument (index 0)
                    UserOrder savedOrder = invocation.getArgument(0);

                    // CRITICAL: Ensure the returned object has the ID set,
                    // which Hibernate would do on the first save.
                    if (savedOrder.getId() == null) {
                        savedOrder.setId(TEST_ORDER_ID);
                    }
                    return savedOrder;
                });

        // Arrange
        when(financeService.checkout(any(UserOrderRequest.class))).thenReturn(MOCK_PAYMENT_ID);
        when(authenticationService.tryGetCurrentUser()).thenReturn(Optional.of(mockUser));

        // --- FIX 2: Use an Argument Captor to verify the final saved state ---
        ArgumentCaptor<UserOrder> orderCaptor = ArgumentCaptor.forClass(UserOrder.class);

        // 2. Act
        UserOrder resultOrder = orderService.createOrder(mockRequest);

        // 3. Assert
        assertNotNull(resultOrder);

        // Assert the paymentId on the returned object
        assertEquals(MOCK_PAYMENT_ID, resultOrder.getPaymentId()); // THIS WILL NOW PASS

        // Assert the final status on the returned object
        assertEquals(ORDER_STATUS.PROCESSING, resultOrder.getStatus(), "Final status must be PROCESSING");

        // Capture the arguments passed to orderRepo.save()
        verify(orderRepo, times(2)).save(orderCaptor.capture());

        // Verify the state of the FINAL (second) saved order
        UserOrder finalSavedOrder = orderCaptor.getAllValues().get(1);
        assertEquals(ORDER_STATUS.PROCESSING, finalSavedOrder.getStatus(),
                "The final saved order must be PROCESSING");
        assertEquals(MOCK_PAYMENT_ID, finalSavedOrder.getPaymentId(),
                "The payment ID must be preserved in the final save");

        // Verify other key service calls
        verify(financeService, times(1)).checkout(mockRequest);
        verify(authenticationService, times(1)).tryGetCurrentUser();
        verify(inventoryService, times(1)).reserveStock(
                mockOrderItem.getProductId(), mockOrderItem.getQuantity(), TEST_ORDER_ID);
    }

    @Test
    void createOrder_PaymentFails_ThrowsPaymentFailedException() throws Exception {
        // Arrange
        when(financeService.checkout(any(UserOrderRequest.class))).thenThrow(new FinancialOperationException());

        // Act & Assert
        assertThrows(PaymentFailedException.class, () -> {
            orderService.createOrder(mockRequest);
        });

        // Verify that no further operations occurred
        verify(authenticationService, never()).tryGetCurrentUser();
        verify(orderRepo, never()).save(any(UserOrder.class));
    }

    @Test
    void createOrder_UserNotLoggedIn_ThrowsUserNotExistsException() throws Exception {
        // Arrange
        when(financeService.checkout(any(UserOrderRequest.class))).thenReturn(MOCK_PAYMENT_ID);
        when(authenticationService.tryGetCurrentUser()).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            orderService.createOrder(mockRequest);
        });

        // Verify only checkout succeeded and no saves occurred
        verify(financeService, times(1)).checkout(mockRequest);
        verify(orderRepo, never()).save(any(UserOrder.class));
    }

    @Test
    void createOrder_StockFails_OrderIsCancelled() throws Exception {
        // Arrange
        when(financeService.checkout(any())).thenReturn(MOCK_PAYMENT_ID);
        when(authenticationService.tryGetCurrentUser()).thenReturn(Optional.of(mockUser));

        // Mock the first save (in generateOrder)
        when(orderRepo.save(any(UserOrder.class)))
                .thenReturn(mockOrder);

        // Mock Stock Failure after first save
        doThrow(new NotEnoughStock("Out of stock"))
                .when(inventoryService).reserveStock(any(), anyInt(), any());

        // Act & Assert
        assertThrows(NotEnoughStock.class, () -> {
            orderService.createOrder(mockRequest);
        });

        // Verify status was set to CANCELLED and saved
        ArgumentCaptor<UserOrder> orderCaptor = ArgumentCaptor.forClass(UserOrder.class);

        // Should be 2 saves: 1st PENDING_RESERVATION, 2nd CANCELLED
        verify(orderRepo, times(2)).save(orderCaptor.capture());

        UserOrder finalSavedOrder = orderCaptor.getAllValues().get(1); // The second saved state
        assertEquals(ORDER_STATUS.CANCELLED, finalSavedOrder.getStatus(),
                "Order must be saved as CANCELLED after stock failure");
    }

    // ===================================
    // TEST: getOrdersForCurrentUser
    // ===================================

    @Test
    void getOrdersForCurrentUser_Success_ReturnsOrderList() {
        // Arrange
        List<UserOrder> expectedList = List.of(mockOrder);
        when(authenticationService.tryGetCurrentUser()).thenReturn(Optional.of(mockUser));
        when(orderRepo.findByLocalUser(mockUser)).thenReturn(expectedList);

        // Act
        List<UserOrder> result = orderService.getOrdersForCurrentUser();

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(orderRepo, times(1)).findByLocalUser(mockUser);
    }

    @Test
    void getOrdersForCurrentUser_UserNotFound_ThrowsException() {
        // Arrange
        when(authenticationService.tryGetCurrentUser()).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            orderService.getOrdersForCurrentUser();
        });

        verify(orderRepo, never()).findByLocalUser(any());
    }

    // ===================================
    // TEST: dispatchOrder
    // ===================================

    @Test
    void dispatchOrder_SuccessAsOwner_ReturnsDispatchedOrder() throws Exception {
        // Arrange
        when(orderRepo.findById(TEST_ORDER_ID)).thenReturn(Optional.of(mockOrder));
        when(authenticationService.isAdmin()).thenReturn(false);
        when(authenticationService.tryGetCurrentUser()).thenReturn(Optional.of(mockUser));
        when(orderRepo.save(any(UserOrder.class))).thenReturn(mockOrder);

        // mockOrder is already set up to have user ID TEST_USER_ID, matching mockUser ID.

        // No exceptions thrown by inventoryService.dispatchStock()

        // Act
        UserOrder result = orderService.dispatchOrder(TEST_ORDER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(ORDER_STATUS.DISPATCHED, result.getStatus());

        verify(orderRepo, times(1)).findById(TEST_ORDER_ID);
        verify(inventoryService, times(1)).dispatchStock(
                mockOrderItem.getProductId(),
                mockOrderItem.getQuantity(),
                TEST_ORDER_ID,
                mockAddress.getId()
        );
        // Verify final save was called
        verify(orderRepo, times(1)).save(any(UserOrder.class));
    }

    @Test
    void dispatchOrder_SuccessAsAdmin_BypassesOwnerCheck() {
        // Arrange
        LocalUser adminUser = new LocalUser();
        adminUser.setId(99L); // Different ID than order owner

        when(orderRepo.findById(TEST_ORDER_ID)).thenReturn(Optional.of(mockOrder));
        when(authenticationService.isAdmin()).thenReturn(true); // ADMIN BYPASS
        when(orderRepo.save(any(UserOrder.class))).thenReturn(mockOrder);

        // Note: tryGetCurrentUser is NOT called because isAdmin() returned true.

        // Act
        UserOrder result = orderService.dispatchOrder(TEST_ORDER_ID);

        // Assert
        assertEquals(ORDER_STATUS.DISPATCHED, result.getStatus());
        verify(authenticationService, times(1)).isAdmin();
        verify(authenticationService, never()).tryGetCurrentUser(); // Important check for admin bypass
    }

    @Test
    void dispatchOrder_OrderNotFound_ThrowsIllegalArgumentException() {
        // Arrange
        when(orderRepo.findById(TEST_ORDER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.dispatchOrder(TEST_ORDER_ID);
        });

        // Verify only findById was called
        verify(authenticationService, never()).isAdmin();
        verify(orderRepo, never()).save(any(UserOrder.class));
    }

    @Test
    void dispatchOrder_NotProcessingStatus_ThrowsIllegalStateException() {
        // Arrange
        mockOrder.setStatus(ORDER_STATUS.PENDING_RESERVATION); // Set to invalid status
        when(orderRepo.findById(TEST_ORDER_ID)).thenReturn(Optional.of(mockOrder));

        // Bypass checks
        when(authenticationService.isAdmin()).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            orderService.dispatchOrder(TEST_ORDER_ID);
        });

        // Verify status check failure
        verify(inventoryService, never()).dispatchStock(any(), anyInt(), any(), any());
    }

    @Test
    void dispatchOrder_OwnerMismatch_ThrowsUnauthorizedAccessException() {
        // Arrange
        LocalUser wrongUser = new LocalUser();
        wrongUser.setId(99L); // Different ID than order owner (1L)

        when(orderRepo.findById(TEST_ORDER_ID)).thenReturn(Optional.of(mockOrder));
        when(authenticationService.isAdmin()).thenReturn(false);
        when(authenticationService.tryGetCurrentUser()).thenReturn(Optional.of(wrongUser));

        // Act & Assert
        assertThrows(UnauthorizedAccessException.class, () -> {
            orderService.dispatchOrder(TEST_ORDER_ID);
        });

        // Verify failure occurred before stock dispatch
        verify(inventoryService, never()).dispatchStock(any(), anyInt(), any(), any());
    }
}
