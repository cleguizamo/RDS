package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.Order;
import com.rds.app_restaurante.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    List<Order> findByDate(LocalDate date);
    List<Order> findByStatus(boolean status);
    List<Order> findByDateBetween(LocalDate startDate, LocalDate endDate);
    List<Order> findByPaymentStatus(PaymentStatus paymentStatus);
    
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.date BETWEEN :startDate AND :endDate AND o.status = true")
    BigDecimal getTotalRevenueBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.date BETWEEN :startDate AND :endDate")
    Long countOrdersBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

