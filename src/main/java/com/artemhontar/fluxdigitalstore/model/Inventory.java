package com.artemhontar.fluxdigitalstore.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inventory")
public class Inventory {
    @Id
    @Column(name = "book_id", nullable = false)
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "warehouse_stock", nullable = false)
    private int warehouseStock = 0;

    @Column(name = "on_hold_stock", nullable = false)
    private int onHoldStock = 0;

    @Column(name = "min_threshold")
    private int minThreshold = 5;

}