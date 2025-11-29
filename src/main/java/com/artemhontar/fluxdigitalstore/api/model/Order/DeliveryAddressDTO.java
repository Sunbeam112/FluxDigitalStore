package com.artemhontar.fluxdigitalstore.api.model.Order;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Builder
public class DeliveryAddressDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String country;
    private String zipCode;
}