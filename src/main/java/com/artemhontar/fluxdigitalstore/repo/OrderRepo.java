package com.artemhontar.fluxdigitalstore.repo;

import com.artemhontar.fluxdigitalstore.model.LocalUser;
import com.artemhontar.fluxdigitalstore.model.UserOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepo extends JpaRepository<UserOrder, Long> {

    @Override
    Optional<UserOrder> findById(Long aLong);

    List<UserOrder> findByLocalUser(LocalUser localUser);
}
