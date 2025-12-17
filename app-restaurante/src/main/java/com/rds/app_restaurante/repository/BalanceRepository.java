package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BalanceRepository extends JpaRepository<Balance, Long> {
    // Solo debe haber un registro de balance
    Optional<Balance> findFirstByOrderByIdAsc();
}

