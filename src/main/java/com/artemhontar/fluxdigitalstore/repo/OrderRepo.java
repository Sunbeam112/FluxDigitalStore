package com.artemhontar.fluxdigitalstore.repo;

import com.artemhontar.fluxdigitalstore.model.UserOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepo extends JpaRepository<UserOrder, Long> {
}
