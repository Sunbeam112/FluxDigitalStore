package com.artemhontar.fluxdigitalstore.api.model.Book;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@Builder
public class BookDTO {
    private Long id;
    private String title;
    private Set<String> authorNames;
    private String isbn;
    private BigDecimal price;
    private Integer publicationYear;
    private String shortDescription;
    private String category;
    private String subcategory;
    private Set<String> categoryNames;
    private String urlPhoto;
}
