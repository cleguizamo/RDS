package com.rds.app_restaurante.model;

public enum AlertType {
    LOW_BALANCE,        // Saldo insuficiente para un pago específico
    BALANCE_THRESHOLD,  // Saldo por debajo del umbral configurado
    PENDING_PAYMENTS    // Hay pagos pendientes
}

