package com.rds.app_restaurante.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class DeliveryRequest {
    @NotNull(message = "El ID del usuario es obligatorio")
    private Long userId;

    @NotEmpty(message = "El pedido debe tener al menos un producto")
    private List<OrderItemRequest> items;

    @NotBlank(message = "La dirección de entrega es obligatoria")
    private String deliveryAddress;

    @NotNull(message = "El teléfono de entrega es obligatorio")
    @Min(value = 1, message = "El teléfono debe ser válido")
    private Long deliveryPhone;
}

