package com.rds.app_restaurante.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatsResponse {
    private LocalDate date;
    private BigDecimal revenue;
    private BigDecimal expenses;
    private BigDecimal profit;
    private Long ordersCount;
    private Long deliveriesCount;
}

