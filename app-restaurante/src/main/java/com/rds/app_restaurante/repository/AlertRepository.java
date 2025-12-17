package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.Alert;
import com.rds.app_restaurante.model.AlertStatus;
import com.rds.app_restaurante.model.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByStatusOrderByCreatedAtDesc(AlertStatus status);
    
    List<Alert> findAllByOrderByCreatedAtDesc();
    
    List<Alert> findByAlertTypeAndStatus(AlertType alertType, AlertStatus status);
}

