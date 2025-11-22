package com.artemhontar.fluxdigitalstore.repo;

import com.artemhontar.fluxdigitalstore.model.ResetPasswordToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RPTRepository extends JpaRepository<ResetPasswordToken, Long> {
    ResetPasswordToken getByTokenIgnoreCase(String token);

    @Override
    boolean existsById(Long aLong);

    void flush();
}
