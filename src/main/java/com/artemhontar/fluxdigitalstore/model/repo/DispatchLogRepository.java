package com.artemhontar.fluxdigitalstore.model.repo;

import com.artemhontar.fluxdigitalstore.model.DispatchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DispatchLogRepository extends JpaRepository<DispatchLog, Long> {
}
