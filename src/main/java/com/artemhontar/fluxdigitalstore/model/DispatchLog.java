package com.artemhontar.fluxdigitalstore.model;

import jakarta.persistence.*;

import java.sql.Timestamp;

import lombok.*;

@Entity
@Table(name = "dispatch_log")
@Getter
@Setter
@NoArgsConstructor
public class DispatchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;


    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "book_isbn", nullable = false, length = 13)
    private String bookIsbn;

    @Column(name = "book_name")
    private String itemName;

    @Column(name = "dispatched_quantity", nullable = false)
    private int dispatchedQuantity;


    @Column(name = "order_id", nullable = false)
    private Long orderId;


    @Column(name = "delivery_address_id", nullable = false)
    private Long deliveryAddressId;

    @Column(name = "dispatch_timestamp", nullable = false)
    private Timestamp dispatchTimestamp = new Timestamp(System.currentTimeMillis());

    @Column(name = "tracking_number", length = 255)
    private String trackingNumber;


}