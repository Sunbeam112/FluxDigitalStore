package com.artemhontar.fluxdigitalstore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

@Entity
@Getter
@Setter
@Table(name = "authority")
public class Authority implements GrantedAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "authority_name", nullable = false, unique = true)
    private String authorityName;

    @Override
    public String getAuthority() {
        return authorityName;
    }
}