package com.rds.app_restaurante.dto;

import com.rds.app_restaurante.model.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    @NotNull(message = "El ID del usuario es obligatorio")
    private Long userId;

    @NotEmpty(message = "El pedido debe tener al menos un producto")
    private List<OrderItemRequest> items;

    @NotNull(message = "El número de mesa es obligatorio")
    @Min(value = 1, message = "El número de mesa debe ser mayor a 0")
    private Integer tableNumber;

    private PaymentMethod paymentMethod; // Opcional, por defecto será CASH para pedidos en mesa

    private String paymentProofUrl; // URL de la imagen del comprobante (opcional, se sube después)
}

