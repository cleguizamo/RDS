package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.ExpenseRequest;
import com.rds.app_restaurante.dto.ExpenseResponse;
import com.rds.app_restaurante.dto.ExpenseSearchRequest;
import com.rds.app_restaurante.model.Expense;
import com.rds.app_restaurante.repository.ExpenseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final BalanceService balanceService;

    // Constructor con BalanceService opcional para evitar problemas si las tablas no existen
    public ExpenseService(ExpenseRepository expenseRepository, BalanceService balanceService) {
        this.expenseRepository = expenseRepository;
        this.balanceService = balanceService;
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getAllExpenses() {
        log.debug("Fetching all expenses from database");
        return expenseRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getAllExpensesPaginated(Pageable pageable) {
        log.debug("Fetching expenses page: {} with size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Expense> expensePage = expenseRepository.findAll(pageable);
        List<ExpenseResponse> expenseResponses = expensePage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(expenseResponses, pageable, expensePage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> searchExpenses(ExpenseSearchRequest searchRequest) {
        log.debug("Searching expenses with filters: {}", searchRequest);
        
        // Configurar paginación y ordenamiento
        int page = searchRequest.getPage() != null ? searchRequest.getPage() : 0;
        int size = searchRequest.getSize() != null ? searchRequest.getSize() : 20;
        
        Sort sort = Sort.unsorted();
        if (searchRequest.getSortBy() != null && !searchRequest.getSortBy().isEmpty()) {
            Sort.Direction direction = Sort.Direction.ASC;
            if (searchRequest.getSortDirection() != null && 
                searchRequest.getSortDirection().equalsIgnoreCase("DESC")) {
                direction = Sort.Direction.DESC;
            }
            sort = Sort.by(direction, searchRequest.getSortBy());
        } else {
            // Por defecto, ordenar por fecha descendente
            sort = Sort.by(Sort.Direction.DESC, "expenseDate");
        }
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Ejecutar búsqueda
        Page<Expense> expenses = expenseRepository.searchExpenses(
                searchRequest.getDescription(),
                searchRequest.getCategory(),
                searchRequest.getPaymentMethod(),
                searchRequest.getStartDate(),
                searchRequest.getEndDate(),
                searchRequest.getMinAmount(),
                searchRequest.getMaxAmount(),
                pageable
        );
        
        log.debug("Found {} expenses matching search criteria", expenses.getTotalElements());
        List<ExpenseResponse> expenseResponses = expenses.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(expenseResponses, pageable, expenses.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByDateRange(LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByExpenseDateBetween(startDate, endDate).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByCategory(String category) {
        return expenseRepository.findByCategory(category).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado con id: " + id));
        return mapToResponse(expense);
    }

    @Transactional
    @CacheEvict(value = "statistics", allEntries = true)
    public ExpenseResponse createExpense(ExpenseRequest expenseRequest) {
        log.info("Creating new expense: {} - {}", expenseRequest.getCategory(), expenseRequest.getDescription());
        Expense expense = new Expense(
                expenseRequest.getDescription(),
                expenseRequest.getCategory(),
                expenseRequest.getAmount(),
                expenseRequest.getExpenseDate(),
                expenseRequest.getPaymentMethod(),
                expenseRequest.getNotes()
        );
        
        if (expenseRequest.getReceiptUrl() != null) {
            expense.setReceiptUrl(expenseRequest.getReceiptUrl());
        }
        
        Expense savedExpense = expenseRepository.save(expense);
        
        // Registrar transacción en el balance (excepto para gastos de sueldo que se registran por separado)
        // Solo intentar registrar si las tablas de balance existen y están disponibles
        if (!"Nómina".equals(expenseRequest.getCategory()) && balanceService != null) {
            try {
                // Verificar que el balance service esté disponible antes de registrar
                balanceService.recordExpense(
                        savedExpense.getAmount(),
                        savedExpense.getDescription(),
                        savedExpense.getId(),
                        "EXPENSE",
                        savedExpense.getNotes()
                );
            } catch (Exception e) {
                // Si falla, solo loguear el error pero no fallar la creación del gasto
                log.warn("No se pudo registrar el gasto en el balance (puede ser normal si las tablas aún no existen): {}", e.getMessage());
            }
        }
        
        log.info("Expense created successfully with ID: {}", savedExpense.getId());
        return mapToResponse(savedExpense);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest expenseRequest) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado con id: " + id));

        expense.setDescription(expenseRequest.getDescription());
        expense.setCategory(expenseRequest.getCategory());
        expense.setAmount(expenseRequest.getAmount());
        expense.setExpenseDate(expenseRequest.getExpenseDate());
        expense.setPaymentMethod(expenseRequest.getPaymentMethod());
        expense.setNotes(expenseRequest.getNotes());
        
        if (expenseRequest.getReceiptUrl() != null) {
            expense.setReceiptUrl(expenseRequest.getReceiptUrl());
        }

        Expense updatedExpense = expenseRepository.save(expense);
        return mapToResponse(updatedExpense);
    }

    @Transactional
    @CacheEvict(value = "statistics", allEntries = true)
    public void deleteExpense(Long id) {
        // Verificar que el gasto existe
        if (!expenseRepository.existsById(id)) {
            throw new RuntimeException("Gasto no encontrado con id: " + id);
        }
        
        // Eliminar la transacción correspondiente en el balance si existe
        if (balanceService != null) {
            try {
                List<com.rds.app_restaurante.model.Transaction> transactions = 
                        balanceService.getTransactionsByReference("EXPENSE", id);
                
                if (!transactions.isEmpty()) {
                    log.info("Eliminando {} transacciones asociadas al gasto {}", transactions.size(), id);
                    // Eliminar todas las transacciones relacionadas (normalmente debería ser solo una)
                    // deleteTransaction ya recalcula el balance internamente, solo necesitamos llamarlo para cada transacción
                    for (com.rds.app_restaurante.model.Transaction transaction : transactions) {
                        balanceService.deleteTransaction(transaction.getId());
                    }
                    log.info("Transacciones eliminadas y balance recalculado después de eliminar el gasto {}", id);
                } else {
                    log.debug("No se encontraron transacciones para el gasto {}", id);
                }
            } catch (Exception e) {
                log.warn("Error al eliminar transacción del balance para el gasto {}: {}", id, e.getMessage());
                // Continuar con la eliminación del gasto aunque falle la eliminación de la transacción
            }
        }
        
        // Eliminar el gasto
        expenseRepository.deleteById(id);
        log.info("Gasto eliminado exitosamente: ID {}", id);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalExpensesBetweenDates(LocalDate startDate, LocalDate endDate) {
        BigDecimal total = expenseRepository.getTotalExpensesBetweenDates(startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional
    @CacheEvict(value = "statistics", allEntries = true)
    public ExpenseResponse createSalaryExpense(com.rds.app_restaurante.model.Employee employee, BigDecimal amount, LocalDate paymentDate) {
        String description = String.format("Pago de sueldo - %s %s", employee.getName(), employee.getLastName());
        Expense expense = new Expense(
                description,
                "Nómina",
                amount,
                paymentDate,
                "Transferencia",
                String.format("Pago automático de sueldo para empleado ID: %d", employee.getId())
        );
        
        Expense savedExpense = expenseRepository.save(expense);
        
        // NO registrar aquí en balance porque ya se registró en SalaryPaymentService
        // para tener mejor control del estado del pago
        
        log.info("Gasto de sueldo creado automáticamente: ID {} - Empleado {} - Monto {}", 
                savedExpense.getId(), employee.getId(), amount);
        return mapToResponse(savedExpense);
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .description(expense.getDescription())
                .category(expense.getCategory())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .paymentMethod(expense.getPaymentMethod())
                .notes(expense.getNotes())
                .receiptUrl(expense.getReceiptUrl())
                .build();
    }
}

