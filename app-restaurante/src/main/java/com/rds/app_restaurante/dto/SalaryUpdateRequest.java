package com.rds.app_restaurante.dto;

import com.rds.app_restaurante.model.PaymentFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SalaryUpdateRequest {
    @NotNull(message = "El sueldo es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El sueldo debe ser mayor a 0")
    private BigDecimal salary;

    @NotNull(message = "La frecuencia de pago es obligatoria")
    private PaymentFrequency paymentFrequency;

    @NotNull(message = "El día de pago es obligatorio")
    @Min(value = 1, message = "El día de pago debe ser entre 1 y 31")
    @Max(value = 31, message = "El día de pago debe ser entre 1 y 31")
    private Integer paymentDay;
}

