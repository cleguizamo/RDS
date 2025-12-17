package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.ExpenseRequest;
import com.rds.app_restaurante.dto.ExpenseResponse;
import com.rds.app_restaurante.dto.ExpenseSearchRequest;
import com.rds.app_restaurante.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/expenses")
@RequiredArgsConstructor

public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            // Parámetros de búsqueda avanzada
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        
        // Si hay algún filtro de búsqueda avanzada (además de fechas), usar búsqueda avanzada
        if (description != null || category != null || paymentMethod != null ||
            minAmount != null || maxAmount != null || sortBy != null) {
            
            ExpenseSearchRequest searchRequest = ExpenseSearchRequest.builder()
                    .description(description)
                    .category(category)
                    .paymentMethod(paymentMethod)
                    .startDate(startDate)
                    .endDate(endDate)
                    .minAmount(minAmount)
                    .maxAmount(maxAmount)
                    .page(page)
                    .size(size)
                    .sortBy(sortBy)
                    .sortDirection(sortDirection)
                    .build();
            
            Page<ExpenseResponse> expensesPage = expenseService.searchExpenses(searchRequest);
            return ResponseEntity.ok(expensesPage);
        }
        
        // Sin filtros avanzados, comportamiento normal
        if (startDate != null && endDate != null) {
            List<ExpenseResponse> expenses = expenseService.getExpensesByDateRange(startDate, endDate);
            return ResponseEntity.ok(expenses);
        } else if (page == 0 && size == 20) {
            // Sin paginación (compatibilidad)
            List<ExpenseResponse> expenses = expenseService.getAllExpenses();
            return ResponseEntity.ok(expenses);
        } else {
            // Con paginación
            Pageable pageable = PageRequest.of(page, size);
            Page<ExpenseResponse> expensesPage = expenseService.getAllExpensesPaginated(pageable);
            return ResponseEntity.ok(expensesPage);
        }
    }
    
    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> searchExpenses(@RequestBody ExpenseSearchRequest searchRequest) {
        try {
            Page<ExpenseResponse> expensesPage = expenseService.searchExpenses(searchRequest);
            return ResponseEntity.ok(expensesPage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error en la búsqueda: " + e.getMessage()));
        }
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByCategory(@PathVariable(value = "category") String category) {
        List<ExpenseResponse> expenses = expenseService.getExpensesByCategory(category);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getExpenseById(@PathVariable(value = "id") Long id) {
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
    public ResponseEntity<?> updateExpense(@PathVariable(value = "id") Long id, @Valid @RequestBody ExpenseRequest expenseRequest) {
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
    public ResponseEntity<?> deleteExpense(@PathVariable(value = "id") Long id) {
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

