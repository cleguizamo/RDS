package com.rds.app_restaurante.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseRequest {
    @NotBlank(message = "La descripción es obligatoria")
    private String description;

    @NotBlank(message = "La categoría es obligatoria")
    private String category;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate expenseDate;

    private String paymentMethod;

    private String notes;

    private String receiptUrl;
}

