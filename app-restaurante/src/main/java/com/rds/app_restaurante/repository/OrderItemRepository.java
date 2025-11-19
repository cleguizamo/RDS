package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
    
    @Query("SELECT oi.product.id, oi.product.name, SUM(oi.quantity), SUM(oi.subtotal) " +
           "FROM OrderItem oi JOIN oi.order o " +
           "WHERE o.date BETWEEN :startDate AND :endDate AND o.status = true " +
           "GROUP BY oi.product.id, oi.product.name " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> getTopProductsBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

