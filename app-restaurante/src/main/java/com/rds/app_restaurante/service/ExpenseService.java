package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.ExpenseRequest;
import com.rds.app_restaurante.dto.ExpenseResponse;
import com.rds.app_restaurante.model.Expense;
import com.rds.app_restaurante.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getAllExpenses() {
        return expenseRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
    public ExpenseResponse createExpense(ExpenseRequest expenseRequest) {
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
    public void deleteExpense(Long id) {
        if (!expenseRepository.existsById(id)) {
            throw new RuntimeException("Gasto no encontrado con id: " + id);
        }
        expenseRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalExpensesBetweenDates(LocalDate startDate, LocalDate endDate) {
        BigDecimal total = expenseRepository.getTotalExpensesBetweenDates(startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
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

