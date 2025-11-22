package com.artemhontar.fluxdigitalstore.api.model.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


import jakarta.validation.constraints.Size;

@Getter
@Setter
public class PasswordResetRequest {
    @NotNull
    @NotBlank
    @Size(min = 8, max = 64)
    private String newPassword;
    @NotBlank
    @NotNull
    private String token;
}
