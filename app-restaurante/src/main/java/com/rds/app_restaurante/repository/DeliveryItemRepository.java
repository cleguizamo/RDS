package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.DeliveryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DeliveryItemRepository extends JpaRepository<DeliveryItem, Long> {
    List<DeliveryItem> findByDeliveryId(Long deliveryId);
    
    @Query("SELECT di.product.id, di.product.name, SUM(di.quantity), SUM(di.subtotal) " +
           "FROM DeliveryItem di JOIN di.delivery d " +
           "WHERE d.date BETWEEN :startDate AND :endDate AND d.status = true " +
           "GROUP BY di.product.id, di.product.name " +
           "ORDER BY SUM(di.quantity) DESC")
    List<Object[]> getTopProductsBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

