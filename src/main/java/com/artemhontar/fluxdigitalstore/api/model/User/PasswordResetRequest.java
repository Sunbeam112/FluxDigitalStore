package com.artemhontar.fluxdigitalstore.api.model.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {
    @NotNull
    @NotBlank
    @Size(min = 8, max = 64)
    private String newPassword;
    @NotBlank
    @NotNull
    private String token;
}
