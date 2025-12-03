package com.artemhontar.fluxdigitalstore.model.repo;

import com.artemhontar.fluxdigitalstore.model.LocalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<LocalUser, Long> {

    Optional<LocalUser> findByEmailIgnoreCase(String emailFromToken);
}
