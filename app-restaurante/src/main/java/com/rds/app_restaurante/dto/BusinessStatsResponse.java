package com.rds.app_restaurante.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessStatsResponse {
    // Estadísticas generales
    private Long totalOrders;
    private Long totalDeliveries;
    private Long totalReservations;
    private Long totalCustomers;
    private Long totalProducts;
    
    // Productos más vendidos
    private List<TopProductResponse> topProducts;
    
    // Clientes más frecuentes
    private List<TopCustomerResponse> topCustomers;
    
    // Estadísticas de hoy
    private DailySummaryResponse todayStats;
    
    // Estadísticas del mes actual
    private MonthlySummaryResponse monthlyStats;
}

