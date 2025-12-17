package com.rds.app_restaurante.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "balance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Balance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "current_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @Column(name = "low_balance_threshold", nullable = false, precision = 15, scale = 2)
    private BigDecimal lowBalanceThreshold;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    // Constructor para inicializar balance
    public Balance(BigDecimal initialBalance, BigDecimal lowBalanceThreshold) {
        this.currentBalance = initialBalance;
        this.lowBalanceThreshold = lowBalanceThreshold;
        this.lastUpdated = LocalDateTime.now();
    }
}

