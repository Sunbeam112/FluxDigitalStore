package com.artemhontar.fluxdigitalstore.api.model.Order;

import lombok.*;


@Getter
@Setter
@Builder
@AllArgsConstructor(access = lombok.AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
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