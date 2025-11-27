package com.artemhontar.fluxdigitalstore.repo;

import com.artemhontar.fluxdigitalstore.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
}
