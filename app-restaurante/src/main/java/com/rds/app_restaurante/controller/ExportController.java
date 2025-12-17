package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.ExpenseSearchRequest;
import com.rds.app_restaurante.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/export")
@RequiredArgsConstructor

@PreAuthorize("hasRole('ADMIN')")
public class ExportController {

    private final ExportService exportService;

    @PostMapping("/expenses/excel")
    public ResponseEntity<?> exportExpensesToExcel(@RequestBody(required = false) ExpenseSearchRequest searchRequest) {
        try {
            if (searchRequest == null) {
                searchRequest = new ExpenseSearchRequest();
            }
            
            byte[] excelData = exportService.exportExpensesToExcel(searchRequest);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                    "gastos_" + LocalDate.now() + ".xlsx");
            headers.setContentLength(excelData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al exportar gastos: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics/excel")
    public ResponseEntity<?> exportStatisticsToExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            // Si no se proporcionan fechas, usar el último mes por defecto
            if (startDate == null) {
                startDate = LocalDate.now().minusMonths(1);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            
            byte[] excelData = exportService.exportFinancialStatsToExcel(startDate, endDate);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                    "estadisticas_financieras_" + startDate + "_" + endDate + ".xlsx");
            headers.setContentLength(excelData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al exportar estadísticas: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics/pdf")
    public ResponseEntity<?> exportStatisticsToPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            // Si no se proporcionan fechas, usar el último mes por defecto
            if (startDate == null) {
                startDate = LocalDate.now().minusMonths(1);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            
            byte[] pdfData = exportService.exportFinancialStatsToPdf(startDate, endDate);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                    "estadisticas_financieras_" + startDate + "_" + endDate + ".pdf");
            headers.setContentLength(pdfData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al exportar estadísticas a PDF: " + e.getMessage()));
        }
    }
}

