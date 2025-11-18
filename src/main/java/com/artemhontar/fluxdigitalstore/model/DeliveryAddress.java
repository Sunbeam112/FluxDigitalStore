package com.artemhontar.fluxdigitalstore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "delivery_address")
public class DeliveryAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 128)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 128)
    private String lastName;

    @Column(name = "address_line_1", nullable = false, length = 512)
    private String addressLine1;

    @Column(name = "address_line_2", length = 128)
    private String addressLine2;

    @Column(name = "city", nullable = false, length = 128)
    private String city;

    @Column(name = "country", nullable = false, length = 128)
    private String country;

    @Column(name = "zipcode", nullable = false, length = 32)
    private String zipcode;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "local_user_id")
    private LocalUser localUser;

}