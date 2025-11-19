package com.rds.app_restaurante.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialStatsResponse {
    private BigDecimal totalRevenue; // Ingresos totales (pedidos + entregas)
    private BigDecimal totalExpenses; // Gastos totales
    private BigDecimal netProfit; // Ganancia neta
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Ingresos por tipo
    private BigDecimal ordersRevenue; // Ingresos de pedidos en mesa
    private BigDecimal deliveriesRevenue; // Ingresos de entregas a domicilio
    
    // Gastos por categoría
    private List<CategoryExpenseResponse> expensesByCategory;
    
    // Estadísticas diarias
    private List<DailyStatsResponse> dailyStats;
}

