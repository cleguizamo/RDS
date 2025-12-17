package com.rds.app_restaurante.dto;

import com.rds.app_restaurante.model.PaymentFrequency;
import com.rds.app_restaurante.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryPaymentResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeLastName;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private LocalDateTime createdAt;
    private PaymentFrequency paymentFrequency;
    private PaymentStatus status;
    private LocalDateTime processedAt;
    private String failureReason;
}

