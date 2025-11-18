package com.artemhontar.fluxdigitalstore.api.model.Book;

import lombok.Getter;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Set;

@Getter
public class BookRequest {
    @NotBlank(message = "Title is mandatory")
    @Size(max = 255)
    private String title;

    @NotEmpty(message = "The set of author names cannot be empty")
    private Set<String> authorNames;

    @NotBlank(message = "ISBN is mandatory")
    @Size(min = 13, max = 13, message = "ISBN must be 13 characters")
    private String isbn;

    @NotNull(message = "Price is mandatory")
    @DecimalMin(value = "0.00", inclusive = true, message = "Price must be non-negative")
    private BigDecimal price;

    @NotNull(message = "Publication year is mandatory")
    @Min(value = 1800, message = "Publication year must be after 1800")
    @Max(value = 2030, message = "Publication year is invalid")
    private Integer publicationYear;

    @NotBlank(message = "Description is mandatory")
    @Size(max = 16384)
    private String description;

    @Size(max = 1024)
    private String shortDescription;

    @NotBlank(message = "Category is mandatory")
    private String category;

    private String subcategory;

    private Set<Long> categoryIds;

    @Size(max = 2048)
    private String urlPhoto;
}