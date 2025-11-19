package com.rds.app_restaurante.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RewardProductRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    private String description;

    private String imageUrl;

    @NotNull(message = "Los puntos requeridos son obligatorios")
    @Min(value = 1, message = "Los puntos requeridos deben ser al menos 1")
    private Long pointsRequired;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock debe ser mayor o igual a 0")
    private Integer stock;

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean isActive;
}

