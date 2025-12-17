package com.rds.app_restaurante.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponse {
    private Long id;
    private BigDecimal currentBalance;
    private BigDecimal lowBalanceThreshold;
    private LocalDateTime lastUpdated;
    private boolean isLowBalance;
}

