package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.ExpenseRequest;
import com.rds.app_restaurante.dto.ExpenseResponse;
import com.rds.app_restaurante.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/expenses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ExpenseResponse>> getAllExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<ExpenseResponse> expenses;
        if (startDate != null && endDate != null) {
            expenses = expenseService.getExpensesByDateRange(startDate, endDate);
        } else {
            expenses = expenseService.getAllExpenses();
        }
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByCategory(@PathVariable String category) {
        List<ExpenseResponse> expenses = expenseService.getExpensesByCategory(category);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getExpenseById(@PathVariable Long id) {
        try {
            ExpenseResponse expense = expenseService.getExpenseById(id);
            return ResponseEntity.ok(expense);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createExpense(@Valid @RequestBody ExpenseRequest expenseRequest) {
        try {
            ExpenseResponse expense = expenseService.createExpense(expenseRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(expense);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al crear el gasto"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateExpense(@PathVariable Long id, @Valid @RequestBody ExpenseRequest expenseRequest) {
        try {
            ExpenseResponse expense = expenseService.updateExpense(id, expenseRequest);
            return ResponseEntity.ok(expense);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al actualizar el gasto"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteExpense(@PathVariable Long id) {
        try {
            expenseService.deleteExpense(id);
            return ResponseEntity.ok(Map.of("message", "Gasto eliminado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al eliminar el gasto"));
        }
    }
}

