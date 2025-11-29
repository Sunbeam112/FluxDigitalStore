package com.artemhontar.fluxdigitalstore.api.model.Order;

import com.artemhontar.fluxdigitalstore.model.enums.ORDER_STATUS;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;


@Getter
@Setter
@Builder
public class OrderDTO {
    private Long id;
    private Timestamp date;
    private Long userId;
    private ORDER_STATUS status;
    private DeliveryAddressDTO deliveryAddress;
    private List<OrderItemDTO> orderItems;
}
