package com.artemhontar.fluxdigitalstore.controller;

import com.artemhontar.fluxdigitalstore.api.model.Order.OrderDTO;
import com.artemhontar.fluxdigitalstore.api.model.User.UserOrderRequest;
import com.artemhontar.fluxdigitalstore.exception.NotEnoughStock;
import com.artemhontar.fluxdigitalstore.exception.NotFoundException;
import com.artemhontar.fluxdigitalstore.exception.PaymentFailedException;
import com.artemhontar.fluxdigitalstore.exception.UnauthorizedAccessException;
import com.artemhontar.fluxdigitalstore.model.UserOrder;
import com.artemhontar.fluxdigitalstore.service.Order.OrderConverter;
import com.artemhontar.fluxdigitalstore.service.Order.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for handling user order operations.
 * Manages the REST API endpoints for creating and dispatching orders.
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;
    private final OrderConverter orderConverter;

    public OrderController(OrderService orderService, OrderConverter orderConverter) {
        this.orderService = orderService;
        this.orderConverter = orderConverter;
    }

    /**
     * Handles the checkout process. It attempts to process payment, reserve stock,
     * and persist the order record.
     *
     * @param orderRequest The request body containing order items and delivery details.
     * @return A ResponseEntity containing the created OrderDTO object and HTTP Status 201.
     */
    @PostMapping("/create")
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody UserOrderRequest orderRequest) {
        try {
            UserOrder createdOrder = orderService.createOrder(orderRequest);
            OrderDTO orderDTO = orderConverter.convertToDto(createdOrder);
            return ResponseEntity.status(HttpStatus.CREATED).body(orderDTO);

        } catch (NotFoundException e) {
            // User is not authenticated.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated or logged in.", e);

        } catch (PaymentFailedException e) {
            // Payment processor declined the transaction (HTTP 402 Payment Required).
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Payment failed. Please check payment details.", e);

        } catch (NotEnoughStock e) {
            // Stock reservation failed (HTTP 409 Conflict).
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for one or more items in the order. Order was cancelled.", e);

        } catch (Exception e) {
            // General failure.
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred during order creation. Order cancelled.", e);
        }
    }

    /**
     * Retrieves the list of all orders associated with the currently authenticated user.
     *
     * @return A ResponseEntity containing a list of OrderDTO objects.
     */
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getOrdersForCurrentUser() {
        try {
            // Service returns the list of entities.
            List<UserOrder> orders = orderService.getOrdersForCurrentUser();
            // Convert list of entities to list of DTOs.
            List<OrderDTO> orderDTOs = orders.stream()
                    .map(orderConverter::convertToDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(orderDTOs);

        } catch (NotFoundException e) {
            // Thrown if the current user cannot be identified.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated or logged in.", e);
        }
    }

    /**
     * Initiates the internal process to dispatch an order. This endpoint is secured
     * by checking if the user is the order owner or has the ADMIN role.
     *
     * @param orderId The ID of the order to dispatch.
     * @return A ResponseEntity containing the dispatched OrderDTO object.
     */
    @PostMapping("/dispatch/{orderId}")
    public ResponseEntity<OrderDTO> dispatchOrder(@PathVariable Long orderId) {
        try {
            // Service returns the updated entity.
            UserOrder dispatchedOrder = orderService.dispatchOrder(orderId);
            // Convert the updated entity to DTO.
            OrderDTO orderDTO = orderConverter.convertToDto(dispatchedOrder);
            return ResponseEntity.ok(orderDTO);

        } catch (UnauthorizedAccessException e) {
            // Thrown if the user is not the owner AND not an admin.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e); // 403 Forbidden

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "Order not found with ID: " + orderId, e);

        } catch (IllegalStateException e) {
            // Thrown if the order is not in PROCESSING status or stock inconsistency is detected (400 Bad Request).
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);

        } catch (Exception e) {
            // General failure during dispatch.
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to dispatch order due to an internal error.", e);
        }
    }
}