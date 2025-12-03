package com.artemhontar.fluxdigitalstore.controller;

import com.artemhontar.fluxdigitalstore.api.model.Order.DeliveryAddressDTO;
import com.artemhontar.fluxdigitalstore.api.model.Order.OrderDTO;
import com.artemhontar.fluxdigitalstore.api.model.Order.OrderItemDTO;
import com.artemhontar.fluxdigitalstore.api.model.User.UserOrderRequest;
import com.artemhontar.fluxdigitalstore.api.security.JWTUtils;
import com.artemhontar.fluxdigitalstore.exception.NotEnoughStock;
import com.artemhontar.fluxdigitalstore.exception.NotFoundException;
import com.artemhontar.fluxdigitalstore.exception.UnauthorizedAccessException;
import com.artemhontar.fluxdigitalstore.model.DeliveryAddress;
import com.artemhontar.fluxdigitalstore.model.OrderItem;
import com.artemhontar.fluxdigitalstore.model.UserOrder;
import com.artemhontar.fluxdigitalstore.model.enums.ORDER_STATUS;
import com.artemhontar.fluxdigitalstore.model.repo.UserRepo;
import com.artemhontar.fluxdigitalstore.service.Order.OrderConverter;
import com.artemhontar.fluxdigitalstore.service.Order.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(OrderControllerTest.TestConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderConverter orderConverter;

    private final String BASE_URL = "/order";

    // --- MOCK CONFIGURATION ---
    @TestConfiguration
    static class TestConfig {

        /**
         * Mocks the UserRepo. This satisfies the dependency chain of JWTRequestFilter
         * and avoids the UnsatisfiedDependencyException without using @MockBean.
         */
        @Bean
        public UserRepo userRepo() {
            return mock(UserRepo.class);
        }

        @Bean
        public OrderService orderService() {
            return mock(OrderService.class);
        }

        @Bean
        public OrderConverter orderConverter() {
            return mock(OrderConverter.class);
        }

        @Bean
        public JWTUtils jwtUtils() {
            return mock(JWTUtils.class);
        }
    }

    @BeforeEach
    void setup() {
        reset(orderService, orderConverter);
    }

    // --- UTILITY METHODS FOR MOCK DATA ---

    private UserOrderRequest createMockOrderRequest(boolean valid) {
        DeliveryAddressDTO address = DeliveryAddressDTO.builder()
                .addressLine1("123 Main St")
                .city("Anytown")
                .zipCode("10001")
                .firstName("John").lastName("Doe").country("USA").build();

        List<OrderItemDTO> items = valid ?
                List.of(OrderItemDTO.builder().productId(101L).quantity(2).build()) :
                Collections.emptyList();

        return UserOrderRequest.builder()
                .deliveryAddress(address)
                .orderItems(items)
                .build();
    }

    private UserOrder createMockUserOrder(Long id, ORDER_STATUS status) {
        UserOrder order = new UserOrder();
        order.setId(id);
        order.setUserId(10L);
        order.setStatus(status);
        order.setDate(Timestamp.from(Instant.now()));
        order.setDeliveryAddress(new DeliveryAddress());
        order.setOrderItems(List.of(new OrderItem()));
        return order;
    }

    private OrderDTO createMockOrderDTO(Long id, ORDER_STATUS status) {
        return OrderDTO.builder()
                .id(id)
                .userId(10L)
                .status(status)
                .deliveryAddress(DeliveryAddressDTO.builder().id(1L).build())
                .orderItems(List.of(OrderItemDTO.builder().id(20L).productId(101L).quantity(2).build()))
                .build();
    }

    // -------------------------------------------------------------------------
    // --- POST /order/create Tests (Order Creation) ---
    // -------------------------------------------------------------------------

    @Test
    void createOrder_Returns201Created_OnSuccess() throws Exception {
        UserOrderRequest request = createMockOrderRequest(true);
        UserOrder mockOrder = createMockUserOrder(1L, ORDER_STATUS.PROCESSING);
        OrderDTO mockOrderDTO = createMockOrderDTO(1L, ORDER_STATUS.PROCESSING);

        doNothing().when(orderService).validateOrderRequest(any(UserOrderRequest.class));
        when(orderService.createOrder(any(UserOrderRequest.class))).thenReturn(mockOrder);
        when(orderConverter.convertToDto(eq(mockOrder))).thenReturn(mockOrderDTO);

        mockMvc.perform(post(BASE_URL + "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        verify(orderService, times(1)).validateOrderRequest(any(UserOrderRequest.class));
        verify(orderService, times(1)).createOrder(any(UserOrderRequest.class));
    }

    @Test
    void createOrder_Returns400BadRequest_OnServiceValidationFailure() throws Exception {
        UserOrderRequest request = createMockOrderRequest(true);

        doThrow(new IllegalArgumentException("Invalid product ID: 999"))
                .when(orderService).validateOrderRequest(any(UserOrderRequest.class));

        mockMvc.perform(post(BASE_URL + "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid product ID: 999"));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    void createOrder_Returns400BadRequest_OnAddressNotFound() throws Exception {
        UserOrderRequest request = createMockOrderRequest(true);
        String addressErrorMsg = "A valid delivery address must be provided.";

        when(orderService.createOrder(any(UserOrderRequest.class)))
                .thenThrow(new NotFoundException(addressErrorMsg));

        mockMvc.perform(post(BASE_URL + "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(addressErrorMsg));
    }

    @Test
    void createOrder_Returns401Unauthorized_OnUserNotAuthenticated() throws Exception {
        UserOrderRequest request = createMockOrderRequest(true);
        String authErrorMsg = "User is not logged in!";

        when(orderService.createOrder(any(UserOrderRequest.class)))
                .thenThrow(new NotFoundException(authErrorMsg));

        mockMvc.perform(post(BASE_URL + "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(authErrorMsg));

        verify(orderService, times(1)).createOrder(any(UserOrderRequest.class));
    }


    @Test
    void createOrder_Returns409Conflict_OnNotEnoughStock() throws Exception {
        UserOrderRequest request = createMockOrderRequest(true);

        when(orderService.createOrder(any(UserOrderRequest.class)))
                .thenThrow(new NotEnoughStock("Insufficient stock for Book 101"));

        mockMvc.perform(post(BASE_URL + "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Insufficient stock for one or more items in the order. Order was cancelled."));
    }

    @Test
    void createOrder_Returns500InternalError_OnOtherException() throws Exception {
        UserOrderRequest request = createMockOrderRequest(true);

        when(orderService.createOrder(any(UserOrderRequest.class)))
                .thenThrow(new RuntimeException("DB connection error"));

        mockMvc.perform(post(BASE_URL + "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("An internal error occurred during order creation. Order cancelled."));
    }

    // -------------------------------------------------------------------------
    // --- GET /order Tests (Order Retrieval) ---
    // -------------------------------------------------------------------------

    @Test
    void getOrdersForCurrentUser_Returns200OkWithContent() throws Exception {
        List<UserOrder> mockOrders = List.of(createMockUserOrder(1L, ORDER_STATUS.DISPATCHED), createMockUserOrder(2L, ORDER_STATUS.PROCESSING));
        List<OrderDTO> mockDTOs = List.of(createMockOrderDTO(1L, ORDER_STATUS.DISPATCHED), createMockOrderDTO(2L, ORDER_STATUS.PROCESSING));

        when(orderService.getOrdersForCurrentUser()).thenReturn(mockOrders);
        when(orderConverter.convertToDto(any(UserOrder.class)))
                .thenReturn(mockDTOs.get(0), mockDTOs.get(1));

        mockMvc.perform(get(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(orderService, times(1)).getOrdersForCurrentUser();
    }

    @Test
    void getOrdersForCurrentUser_Returns401Unauthorized() throws Exception {
        when(orderService.getOrdersForCurrentUser())
                .thenThrow(new NotFoundException("No authenticated user found to retrieve orders."));

        mockMvc.perform(get(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(orderService, times(1)).getOrdersForCurrentUser();
    }

    // -------------------------------------------------------------------------
    // --- POST /order/dispatch/{orderId} Tests (Order Dispatch) ---
    // -------------------------------------------------------------------------

    @Test
    void dispatchOrder_Returns200Ok_OnSuccess() throws Exception {
        Long orderId = 5L;
        UserOrder dispatchedOrder = createMockUserOrder(orderId, ORDER_STATUS.DISPATCHED);
        OrderDTO dispatchedDTO = createMockOrderDTO(orderId, ORDER_STATUS.DISPATCHED);

        when(orderService.dispatchOrder(orderId)).thenReturn(dispatchedOrder);
        when(orderConverter.convertToDto(eq(dispatchedOrder))).thenReturn(dispatchedDTO);

        mockMvc.perform(post(BASE_URL + "/dispatch/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("DISPATCHED"));

        verify(orderService, times(1)).dispatchOrder(orderId);
    }

    @Test
    void dispatchOrder_Returns403Forbidden_OnUnauthorizedAccess() throws Exception {
        Long orderId = 5L;
        String expectedReason = "User is not authorized to dispatch this order.";

        when(orderService.dispatchOrder(orderId))
                .thenThrow(new UnauthorizedAccessException(expectedReason));

        mockMvc.perform(post(BASE_URL + "/dispatch/{orderId}", orderId))
                .andExpect(status().isForbidden())

                .andExpect(result -> assertInstanceOf(ResponseStatusException.class, result.getResolvedException()))
                .andExpect(result -> {
                    ResponseStatusException resolvedException = (ResponseStatusException) result.getResolvedException();
                    assertNotNull(resolvedException);
                    assertEquals(expectedReason, resolvedException.getReason());
                    assertEquals(403, resolvedException.getStatusCode().value());
                });

        verify(orderService, times(1)).dispatchOrder(orderId);
    }

    @Test
    void dispatchOrder_Returns404NotFound_OnOrderNotFound() throws Exception {
        Long orderId = 99L;

        when(orderService.dispatchOrder(orderId))
                .thenThrow(new IllegalArgumentException("Order is not found with ID: 99"));

        mockMvc.perform(post(BASE_URL + "/dispatch/{orderId}", orderId))
                .andExpect(status().isNotFound());

        verify(orderService, times(1)).dispatchOrder(orderId);
    }

    @Test
    void dispatchOrder_Returns400BadRequest_OnInvalidOrderStatus() throws Exception {
        Long orderId = 5L;
        String expectedReason = "Order status is not PROCESSING. Cannot dispatch.";

        when(orderService.dispatchOrder(orderId))
                .thenThrow(new IllegalStateException(expectedReason));

        mockMvc.perform(post(BASE_URL + "/dispatch/{orderId}", orderId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(ResponseStatusException.class, result.getResolvedException()))
                .andExpect(result -> {
                    ResponseStatusException resolvedException = (ResponseStatusException) result.getResolvedException();
                    assertNotNull(resolvedException);
                    assertEquals(expectedReason, resolvedException.getReason());
                    assertEquals(400, resolvedException.getStatusCode().value());
                });

        verify(orderService, times(1)).dispatchOrder(orderId);
    }
}