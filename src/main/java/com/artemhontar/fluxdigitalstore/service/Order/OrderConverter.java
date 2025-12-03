package com.artemhontar.fluxdigitalstore.service.Order;

import com.artemhontar.fluxdigitalstore.api.model.Order.DeliveryAddressDTO;
import com.artemhontar.fluxdigitalstore.api.model.Order.OrderDTO;
import com.artemhontar.fluxdigitalstore.api.model.Order.OrderItemDTO;
import com.artemhontar.fluxdigitalstore.exception.NotFoundException; // For book lookup failure
import com.artemhontar.fluxdigitalstore.model.Book; // Required for OrderItem entity
import com.artemhontar.fluxdigitalstore.model.DeliveryAddress;
import com.artemhontar.fluxdigitalstore.model.OrderItem;
import com.artemhontar.fluxdigitalstore.model.UserOrder;
import com.artemhontar.fluxdigitalstore.model.repo.BookRepo; // New Dependency
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Component responsible for converting the UserOrder JPA entity to the public OrderDTO,
 * and now also converting OrderItemDTO (from request) to OrderItem entity.
 */
@Component
@RequiredArgsConstructor
public class OrderConverter {

    private final BookRepo bookRepo; // Assuming you have a BookRepo for fetching product data

    /**
     * Converts a full UserOrder entity to a secure OrderDTO for API transmission.
     *
     * @param order The UserOrder entity to convert.
     * @return The resulting OrderDTO.
     */
    public OrderDTO convertToDto(UserOrder order) {

        OrderDTO.OrderDTOBuilder builder = OrderDTO.builder()
                .id(order.getId())
                .date(order.getDate())
                .status(order.getStatus())
                .userId(order.getUserId());

        if (order.getDeliveryAddress() != null) {
            builder.deliveryAddress(this.convertToDto(order.getDeliveryAddress()));
        }

        if (order.getOrderItems() != null) {
            builder.orderItems(order.getOrderItems().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toUnmodifiableList()));
        }

        return builder.build();
    }

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


    /**
     * Converts an OrderItemDTO (from a request) into an OrderItem JPA entity.
     * This requires looking up the associated Product/Book entity.
     *
     * @param dto The OrderItemDTO containing product ID and quantity.
     * @return The populated OrderItem entity.
     * @throws NotFoundException If the product (Book) specified in the DTO is not found.
     */
    public OrderItem convertToEntity(OrderItemDTO dto) {
        if (dto == null) return null;

        Book book = bookRepo.findById(dto.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found with ID: " + dto.getProductId()));

        OrderItem item = new OrderItem();
        item.setProductId(dto.getProductId());
        item.setQuantity(dto.getQuantity());
        item.setBook(book);
        item.setDispatched(false);

        return item;
    }
}