package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByUserId(Long userId);
    List<Delivery> findByDate(LocalDate date);
    List<Delivery> findByStatus(boolean status);
    List<Delivery> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT SUM(d.totalPrice) FROM Delivery d WHERE d.date BETWEEN :startDate AND :endDate AND d.status = true")
    BigDecimal getTotalRevenueBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.date BETWEEN :startDate AND :endDate")
    Long countDeliveriesBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

