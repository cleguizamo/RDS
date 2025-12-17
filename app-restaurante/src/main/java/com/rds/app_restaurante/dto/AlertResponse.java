package com.rds.app_restaurante.dto;

import com.rds.app_restaurante.model.AlertType;
import com.rds.app_restaurante.model.AlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private Long id;
    private AlertType alertType;
    private AlertStatus status;
    private String message;
    private String severity;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}

