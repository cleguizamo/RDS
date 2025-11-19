package com.rds.app_restaurante.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "La descripción es obligatoria")
    private String description;

    @NotBlank(message = "La URL de la imagen es obligatoria")
    private String imageUrl;

    @NotNull(message = "El precio es obligatorio")
    @Min(value = 0, message = "El precio debe ser mayor o igual a 0")
    private Double price;

    @NotNull(message = "El ID de la categoría es obligatorio")
    private Long categoryId;

    private Long subCategoryId;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock debe ser mayor o igual a 0")
    private Integer stock;
}

