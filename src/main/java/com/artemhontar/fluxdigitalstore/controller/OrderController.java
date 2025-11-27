package com.artemhontar.fluxdigitalstore.controller;

import com.artemhontar.fluxdigitalstore.api.model.User.UserOrderRequest;
import com.artemhontar.fluxdigitalstore.exception.NotEnoughStock;
import com.artemhontar.fluxdigitalstore.exception.PaymentFailedException;
import com.artemhontar.fluxdigitalstore.exception.UnauthorizedAccessException;
import com.artemhontar.fluxdigitalstore.exception.UserNotExistsException;
import com.artemhontar.fluxdigitalstore.model.UserOrder;
import com.artemhontar.fluxdigitalstore.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Controller for handling user order operations.
 * Manages the REST API endpoints for creating and dispatching orders.
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Handles the checkout process. It attempts to process payment, reserve stock,
     * and persist the order record.
     *
     * @param orderRequest The request body containing order items and delivery details.
     * @return A ResponseEntity containing the created UserOrder object and HTTP Status 201.
     */
    @PostMapping("/create")
    public ResponseEntity<UserOrder> createOrder(@Valid @RequestBody UserOrderRequest orderRequest) {
        try {
            // Service handles payment, reservation, and status updates.
            UserOrder createdOrder = orderService.createOrder(orderRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);

        } catch (UserNotExistsException e) {
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
     * @return A ResponseEntity containing a list of UserOrder objects.
     */
    @GetMapping
    public ResponseEntity<List<UserOrder>> getOrdersForCurrentUser() {
        try {
            // Service handles authentication check and data retrieval.
            List<UserOrder> orders = orderService.getOrdersForCurrentUser();
            return ResponseEntity.ok(orders);

        } catch (UserNotExistsException e) {
            // Thrown if the current user cannot be identified.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated or logged in.", e);
        }
    }

    /**
     * Initiates the internal process to dispatch an order. This endpoint is secured
     * by checking if the user is the order owner or has the ADMIN role.
     *
     * @param orderId The ID of the order to dispatch.
     * @return A ResponseEntity containing the dispatched UserOrder object.
     */
    @PostMapping("/dispatch/{orderId}")
    public ResponseEntity<UserOrder> dispatchOrder(@PathVariable Long orderId) {
        try {
            // Service performs security checks, status validation, and stock update.
            UserOrder dispatchedOrder = orderService.dispatchOrder(orderId);
            return ResponseEntity.ok(dispatchedOrder);

        } catch (UnauthorizedAccessException e) {
            // Thrown if the user is not the owner AND not an admin.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e); // 403 Forbidden

        } catch (IllegalArgumentException e) {
            // Thrown if the order is not found (404 Not Found).
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId, e);

        } catch (IllegalStateException e) {
            // Thrown if the order is not in PROCESSING status or stock inconsistency is detected (400 Bad Request).
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);

        } catch (Exception e) {
            // General failure during dispatch.
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to dispatch order due to an internal error.", e);
        }
    }
}