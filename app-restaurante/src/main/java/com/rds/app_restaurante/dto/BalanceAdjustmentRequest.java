package com.rds.app_restaurante.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BalanceAdjustmentRequest {
    @NotNull(message = "El monto es obligatorio")
    private BigDecimal amount;

    @NotNull(message = "La razón es obligatoria")
    private String reason;

    private String notes;
}

