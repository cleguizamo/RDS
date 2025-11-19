package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    List<Order> findByDate(LocalDate date);
    List<Order> findByStatus(boolean status);
}

