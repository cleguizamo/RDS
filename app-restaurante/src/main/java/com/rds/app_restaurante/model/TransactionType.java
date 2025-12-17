package com.rds.app_restaurante.model;

public enum TransactionType {
    INCOME,         // Ingreso - por ventas/pedidos
    EXPENSE,        // Gasto - gasto general
    SALARY_PAYMENT, // Pago de sueldo
    ADJUSTMENT,     // Ajuste manual del balance
    REFUND          // Reembolso
}

