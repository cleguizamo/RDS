package com.rds.app_restaurante.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private String description;
    private String category;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String paymentMethod;
    private String notes;
    private String receiptUrl;
}

