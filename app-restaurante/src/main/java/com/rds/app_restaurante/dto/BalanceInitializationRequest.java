package com.rds.app_restaurante.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BalanceInitializationRequest {
    @NotNull(message = "El saldo inicial es obligatorio")
    @DecimalMin(value = "0.0", message = "El saldo inicial debe ser mayor o igual a 0")
    private BigDecimal initialBalance;

    @NotNull(message = "El umbral de saldo bajo es obligatorio")
    @DecimalMin(value = "0.0", message = "El umbral debe ser mayor o igual a 0")
    private BigDecimal lowBalanceThreshold;
}

