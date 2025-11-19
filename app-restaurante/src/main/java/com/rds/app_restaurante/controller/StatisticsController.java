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
@CrossOrigin(origins = "*")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/financial")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FinancialStatsResponse> getFinancialStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // Si no se proporcionan fechas, usar el último mes por defecto
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        FinancialStatsResponse stats = statisticsService.getFinancialStats(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/business")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusinessStatsResponse> getBusinessStats() {
        BusinessStatsResponse stats = statisticsService.getBusinessStats();
        return ResponseEntity.ok(stats);
    }
}

