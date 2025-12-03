package com.artemhontar.fluxdigitalstore.api.model.User;

import com.artemhontar.fluxdigitalstore.api.model.Order.DeliveryAddressDTO;
import com.artemhontar.fluxdigitalstore.api.model.Order.OrderItemDTO;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserOrderRequest {
    DeliveryAddressDTO deliveryAddress;
    private List<OrderItemDTO> orderItems;

}
