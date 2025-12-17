package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.*;
import com.rds.app_restaurante.model.TransactionType;
import com.rds.app_restaurante.service.AlertService;
import com.rds.app_restaurante.service.BalanceService;
import com.rds.app_restaurante.service.SalaryPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/balance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BalanceController {

    private final BalanceService balanceService;
    private final AlertService alertService;
    private final SalaryPaymentService salaryPaymentService;

    @GetMapping
    public ResponseEntity<BalanceResponse> getCurrentBalance() {
        var balance = balanceService.getCurrentBalance();
        BalanceResponse response = BalanceResponse.builder()
                .id(balance.getId())
                .currentBalance(balance.getCurrentBalance())
                .lowBalanceThreshold(balance.getLowBalanceThreshold())
                .lastUpdated(balance.getLastUpdated())
                .isLowBalance(balanceService.isLowBalance())
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/initialize")
    public ResponseEntity<?> initializeBalance(@Valid @RequestBody BalanceInitializationRequest request) {
        try {
            var balance = balanceService.initializeBalance(
                    request.getInitialBalance(),
                    request.getLowBalanceThreshold()
            );
            BalanceResponse response = BalanceResponse.builder()
                    .id(balance.getId())
                    .currentBalance(balance.getCurrentBalance())
                    .lowBalanceThreshold(balance.getLowBalanceThreshold())
                    .lastUpdated(balance.getLastUpdated())
                    .isLowBalance(false)
                    .build();
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/threshold")
    public ResponseEntity<BalanceResponse> updateThreshold(@RequestBody java.util.Map<String, java.math.BigDecimal> request) {
        var threshold = request.get("threshold");
        if (threshold == null) {
            return ResponseEntity.badRequest().build();
        }
        var balance = balanceService.updateLowBalanceThreshold(threshold);
        BalanceResponse response = BalanceResponse.builder()
                .id(balance.getId())
                .currentBalance(balance.getCurrentBalance())
                .lowBalanceThreshold(balance.getLowBalanceThreshold())
                .lastUpdated(balance.getLastUpdated())
                .isLowBalance(balanceService.isLowBalance())
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/adjust")
    public ResponseEntity<?> adjustBalance(@Valid @RequestBody BalanceAdjustmentRequest request) {
        try {
            var transaction = balanceService.adjustBalance(
                    request.getAmount(),
                    request.getReason(),
                    request.getNotes()
            );
            
            // Verificar alertas después del ajuste
            alertService.checkAndCreateAlerts();
            
            // Intentar procesar pagos pendientes
            salaryPaymentService.processPendingPayments();
            
            TransactionResponse response = mapToTransactionResponse(transaction);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("message", "Error al ajustar el balance: " + e.getMessage()));
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getAllTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size) {
        
        List<com.rds.app_restaurante.model.Transaction> transactions;
        if (type != null) {
            transactions = balanceService.getTransactionsByType(type);
        } else {
            transactions = balanceService.getAllTransactions();
        }

        // Aplicar paginación manual
        int start = page * size;
        int end = Math.min(start + size, transactions.size());
        List<com.rds.app_restaurante.model.Transaction> paginated = 
                transactions.subList(start, end);

        List<TransactionResponse> responses = paginated.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/pending-payments")
    public ResponseEntity<List<SalaryPaymentResponse>> getPendingPayments() {
        return ResponseEntity.ok(salaryPaymentService.getPendingPayments());
    }

    @PostMapping("/process-pending")
    public ResponseEntity<?> processPendingPayments() {
        try {
            salaryPaymentService.processPendingPayments();
            return ResponseEntity.ok(java.util.Map.of("message", "Pagos pendientes procesados"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("message", "Error al procesar pagos pendientes: " + e.getMessage()));
        }
    }

    @PostMapping("/process-salary-payments")
    public ResponseEntity<?> processSalaryPayments() {
        try {
            salaryPaymentService.processSalaryPayments();
            // Después de procesar los pagos, intentar procesar los pendientes
            salaryPaymentService.processPendingPayments();
            // Verificar y crear alertas
            alertService.checkAndCreateAlerts();
            return ResponseEntity.ok(java.util.Map.of("message", "Procesamiento de sueldos completado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("message", "Error al procesar sueldos: " + e.getMessage()));
        }
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<AlertResponse>> getAlerts(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<com.rds.app_restaurante.model.Alert> alerts;
        if (activeOnly) {
            alerts = alertService.getActiveAlerts();
        } else {
            alerts = alertService.getAllAlerts();
        }
        
        List<AlertResponse> responses = alerts.stream()
                .map(this::mapToAlertResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/alerts/{id}/resolve")
    public ResponseEntity<AlertResponse> resolveAlert(@PathVariable("id") Long id) {
        var alert = alertService.resolveAlert(id);
        return ResponseEntity.ok(mapToAlertResponse(alert));
    }

    @PostMapping("/migrate-historical-data")
    public ResponseEntity<?> migrateHistoricalData() {
        try {
            var result = balanceService.migrateHistoricalOrdersAndDeliveries();
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("message", "Migración completada exitosamente");
            response.put("ordersMigrated", result.getOrdersMigrated());
            response.put("deliveriesMigrated", result.getDeliveriesMigrated());
            response.put("expensesMigrated", result.getExpensesMigrated());
            response.put("totalOrdersRevenue", result.getTotalOrdersRevenue());
            response.put("totalDeliveriesRevenue", result.getTotalDeliveriesRevenue());
            response.put("totalExpenses", result.getTotalExpenses());
            response.put("totalRevenue", result.getTotalRevenue());
            response.put("netBalance", result.getNetBalance());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("message", "Error durante la migración: " + e.getMessage()));
        }
    }

    private TransactionResponse mapToTransactionResponse(com.rds.app_restaurante.model.Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .referenceId(transaction.getReferenceId())
                .referenceType(transaction.getReferenceType())
                .notes(transaction.getNotes())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private AlertResponse mapToAlertResponse(com.rds.app_restaurante.model.Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .alertType(alert.getAlertType())
                .status(alert.getStatus())
                .message(alert.getMessage())
                .severity(alert.getSeverity())
                .createdAt(alert.getCreatedAt())
                .resolvedAt(alert.getResolvedAt())
                .build();
    }
}

