package com.artemhontar.fluxdigitalstore.api.model.User;

import com.artemhontar.fluxdigitalstore.model.OrderItem;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class UserOrderRequest {
    private List<OrderItem> orderItems;

}
