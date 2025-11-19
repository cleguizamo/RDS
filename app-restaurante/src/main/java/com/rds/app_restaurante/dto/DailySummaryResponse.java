package com.rds.app_restaurante.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySummaryResponse {
    private Long ordersCount;
    private Long deliveriesCount;
    private Long reservationsCount;
    private BigDecimal revenue;
    private BigDecimal expenses;
    private BigDecimal profit;
}

