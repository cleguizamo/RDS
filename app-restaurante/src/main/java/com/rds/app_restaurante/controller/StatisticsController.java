package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.BusinessStatsResponse;
import com.rds.app_restaurante.dto.FinancialStatsResponse;
import com.rds.app_restaurante.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor

public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/financial")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getFinancialStats(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            // Si no se proporcionan fechas, usar el último mes por defecto
            if (startDate == null) {
                startDate = LocalDate.now().minusMonths(1);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            System.out.println("=== StatisticsController: Fechas recibidas ===");
            System.out.println("startDate: " + startDate);
            System.out.println("endDate: " + endDate);
            System.out.println("hoy: " + LocalDate.now());
            FinancialStatsResponse stats = statisticsService.getFinancialStats(startDate, endDate);
            System.out.println("Total revenue: " + stats.getTotalRevenue());
            System.out.println("Total daily stats: " + stats.getDailyStats().size());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error en getFinancialStats: " + e.getMessage());
            e.printStackTrace();
            java.util.Map<String, String> errorResponse = new java.util.HashMap<>();
            errorResponse.put("error", "Error al obtener estadísticas financieras: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/business")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusinessStatsResponse> getBusinessStats() {
        System.out.println("=== StatisticsController: getBusinessStats llamado ===");
        System.out.println("Timestamp: " + new java.util.Date());
        BusinessStatsResponse stats = statisticsService.getBusinessStats();
        System.out.println("Total pedidos: " + stats.getTotalOrders());
        System.out.println("Total entregas: " + stats.getTotalDeliveries());
        System.out.println("Total reservas: " + stats.getTotalReservations());
        System.out.println("Total clientes: " + stats.getTotalCustomers());
        System.out.println("Pedidos hoy: " + stats.getTodayStats().getOrdersCount());
        System.out.println("Entregas hoy: " + stats.getTodayStats().getDeliveriesCount());
        return ResponseEntity.ok(stats);
    }
}

