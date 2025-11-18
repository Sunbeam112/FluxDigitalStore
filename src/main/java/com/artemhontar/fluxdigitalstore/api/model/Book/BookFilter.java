package com.artemhontar.fluxdigitalstore.api.model.Book;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BookFilter {
    private String title;
    private String author;

    private String isbn;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private Integer minPublicationYear;
    private Integer maxPublicationYear;

    private String category;
}