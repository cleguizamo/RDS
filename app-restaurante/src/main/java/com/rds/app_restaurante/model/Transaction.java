package com.rds.app_restaurante.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "reference_id")
    private Long referenceId; // ID de la entidad relacionada (expense_id, order_id, salary_payment_id, etc.)

    @Column(name = "reference_type", length = 50)
    private String referenceType; // Tipo de referencia (EXPENSE, ORDER, SALARY_PAYMENT, etc.)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

