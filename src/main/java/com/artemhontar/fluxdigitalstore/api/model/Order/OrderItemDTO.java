package com.artemhontar.fluxdigitalstore.api.model.Order;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;


@Getter
@Setter
@Builder
public class OrderItemDTO {
    private Long id;
    private Long productId;
    private int quantity;
    private boolean isDispatched;
    private Timestamp dateDispatched;
}
