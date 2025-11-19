package com.rds.app_restaurante.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "category", nullable = false)
    private String category; // Ej: "Nómina", "Inventario", "Servicios", "Mantenimiento", "Marketing", "Otros"

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "payment_method")
    private String paymentMethod; // Ej: "Efectivo", "Transferencia", "Tarjeta", "Cheque"

    @Column(name = "notes")
    private String notes;

    @Column(name = "receipt_url")
    private String receiptUrl; // URL del comprobante/documento

    public Expense(String description, String category, BigDecimal amount, LocalDate expenseDate, String paymentMethod, String notes) {
        this.description = description;
        this.category = category;
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
    }
}

