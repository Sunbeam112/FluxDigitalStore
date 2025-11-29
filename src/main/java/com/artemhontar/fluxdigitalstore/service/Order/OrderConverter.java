package com.artemhontar.fluxdigitalstore.service.Order;

import com.artemhontar.fluxdigitalstore.api.model.Order.DeliveryAddressDTO;
import com.artemhontar.fluxdigitalstore.api.model.Order.OrderDTO;
import com.artemhontar.fluxdigitalstore.api.model.Order.OrderItemDTO;
import com.artemhontar.fluxdigitalstore.model.DeliveryAddress;
import com.artemhontar.fluxdigitalstore.model.OrderItem;
import com.artemhontar.fluxdigitalstore.model.UserOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Component responsible for converting the UserOrder JPA entity to the public OrderDTO.
 * It now handles the conversion of nested entities internally.
 */
@Component
@RequiredArgsConstructor
public class OrderConverter {

    // Removed the injected dependencies (DeliveryAddressConverter and OrderItemConverter)

    /**
     * Converts a full UserOrder entity to a secure OrderDTO for API transmission.
     *
     * @param order The UserOrder entity to convert.
     * @return The resulting OrderDTO.
     */
    public OrderDTO convertToDto(UserOrder order) {

        // Start building the DTO using the Lombok @Builder pattern
        OrderDTO.OrderDTOBuilder builder = OrderDTO.builder()
                .id(order.getId())
                .date(order.getDate())
                .status(order.getStatus())
                .userId(order.getUserId());

        // Handle nested DeliveryAddress entity conversion using local helper
        if (order.getDeliveryAddress() != null) {
            builder.deliveryAddress(this.convertToDto(order.getDeliveryAddress()));
        }

        // Handle nested List of OrderItem entities conversion using local helper
        if (order.getOrderItems() != null) {
            builder.orderItems(order.getOrderItems().stream()
                    .map(this::convertToDto) // Now using the local helper method reference
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    /**
     * Converts a DeliveryAddress entity to a secure DeliveryAddressDTO.
     * This method was requested as a replacement for the injected converter.
     */
    private DeliveryAddressDTO convertToDto(DeliveryAddress address) {
        if (address == null) return null;

        return DeliveryAddressDTO.builder()
                .id(address.getId())
                .firstName(address.getFirstName())
                .lastName(address.getLastName())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .country(address.getCountry())
                .zipCode(address.getZipcode())
                .build();
    }

    /**
     * Converts an OrderItem entity to a secure OrderItemDTO.
     * This method was requested as a replacement for the injected converter.
     */
    private OrderItemDTO convertToDto(OrderItem item) {
        if (item == null) return null;

        return OrderItemDTO.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .isDispatched(item.isDispatched())
                .dateDispatched(item.getDateDispatched())
                .build();
    }
}