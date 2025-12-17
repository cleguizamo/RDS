package com.rds.app_restaurante.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertType alertType;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = AlertStatus.ACTIVE;
        }
    }
}

