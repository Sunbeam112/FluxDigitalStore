package com.artemhontar.fluxdigitalstore.model.repo;

import com.artemhontar.fluxdigitalstore.model.DeliveryAddress;
import com.artemhontar.fluxdigitalstore.model.LocalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface DeliveryAddressRepo extends JpaRepository<DeliveryAddress, Long> {
    Optional<DeliveryAddress> findByIdAndLocalUser(Long id, LocalUser localUser);

    Optional<DeliveryAddress> findByIdAndLocalUser_Id(Long id, Long localUserId);
}
