package com.artemhontar.fluxdigitalstore.api.model.User;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UserInfoResponse {
    private Long id;
    private String email;
    private boolean isEmailVerified;
    private Set<String> roles;
}
