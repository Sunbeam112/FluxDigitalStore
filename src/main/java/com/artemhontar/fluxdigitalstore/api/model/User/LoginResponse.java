package com.artemhontar.fluxdigitalstore.api.model.User;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String token;
    private boolean success;
    private String failureReason;
}
