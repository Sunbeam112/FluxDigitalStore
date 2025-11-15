package com.artemhontar.fluxdigitalstore.api.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class StringRequest {

    @NotBlank(message = "The input string cannot be empty or blank.")
    private String data;


}
