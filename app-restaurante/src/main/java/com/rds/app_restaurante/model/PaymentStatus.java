package com.rds.app_restaurante.model;

public enum PaymentStatus {
    PENDING,    // Pendiente - esperando verificación del admin
    VERIFIED,   // Verificado - pago verificado por el admin
    REJECTED,   // Rechazado - pago rechazado por el admin
    PAID,       // Pagado - pago completado exitosamente (usado para pagos de sueldo)
    FAILED,     // Fallido - error al procesar el pago
    CANCELLED   // Cancelado - pago cancelado manualmente
}

