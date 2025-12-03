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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for handling user order operations.
 * Manages the REST API endpoints for creating, retrieving, and dispatching orders.
 */
@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final OrderConverter orderConverter;

    public OrderController(OrderService orderService, OrderConverter orderConverter) {
        this.orderService = orderService;
        this.orderConverter = orderConverter;
    }

    // --- ORDER CREATION ---

    /**
     * Handles the checkout process. It attempts to process payment, reserve stock,
     * and persist the order record.
     *
     * @param orderRequest The request body containing order items and delivery details.
     * @return A ResponseEntity containing the created OrderDTO object and HTTP Status 201,
     * or an error message with the appropriate HTTP status.
     */
    @PostMapping("/create")
    public ResponseEntity<Object> createOrder(@Valid @RequestBody UserOrderRequest orderRequest) {
        try {
            // 1. Validation (Product existence, required fields)
            orderService.validateOrderRequest(orderRequest);

            // 2. Order Processing (Payment, Address Assignment, Reservation)
            UserOrder createdOrder = orderService.createOrder(orderRequest);

            // 3. Response Conversion
            OrderDTO orderDTO = orderConverter.convertToDto(createdOrder);

            return ResponseEntity.status(HttpStatus.CREATED).body(orderDTO);

        } catch (IllegalArgumentException e) {
            // Thrown from validateOrderRequest or reserveOrder (e.g., empty order, invalid product ID).
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NotFoundException e) {
            // Thrown if the user is not authenticated OR if the address fallback logic failed (address not found).
            // We use 400 Bad Request for the address failure and 401 Unauthorized for user authentication failure.
            // Since the service logic throws NotFoundException for both user and address, we check the message.
            if (e.getMessage().contains("authenticated")) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED) // 401
                        .body("User is not authenticated or logged in.");
            } else {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST) // 400
                        .body(e.getMessage()); // e.g., "A valid delivery address must be provided..."
            }

        } catch (PaymentFailedException e) {
            // Payment processor declined the transaction.
            return ResponseEntity
                    .status(HttpStatus.PAYMENT_REQUIRED) // 402
                    .body("Payment failed. Please check payment details.");

        } catch (NotEnoughStock e) {
            // Stock reservation failed.
            return ResponseEntity
                    .status(HttpStatus.CONFLICT) // 409
                    .body("Insufficient stock for one or more items in the order. Order was cancelled.");

        } catch (Exception e) {
            // General failure.
            log.error("Unhandled exception during order creation:", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR) // 500
                    .body("An internal error occurred during order creation. Order cancelled.");
        }
    }

    // --- ORDER RETRIEVAL ---

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
                    .collect(Collectors.toUnmodifiableList());

            return ResponseEntity.ok(orderDTOs);

        } catch (NotFoundException e) {
            // Thrown if the current user cannot be identified by AuthenticationService.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated or logged in.", e);
        }
    }

    // --- ORDER DISPATCH (ADMIN/OWNER ONLY) ---

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
            // Thrown if the order ID is not found. We use 404 NOT FOUND for resource ID lookups.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId, e);

        } catch (IllegalStateException e) {
            // Thrown if the order is not in PROCESSING status or stock inconsistency is detected.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e); // 400 Bad Request

        } catch (Exception e) {
            // General failure during dispatch.
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to dispatch order due to an internal error.", e);
        }
    }
}