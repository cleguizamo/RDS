package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.RewardProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardProductRepository extends JpaRepository<RewardProduct, Long> {
    List<RewardProduct> findByIsActiveTrue();
}

