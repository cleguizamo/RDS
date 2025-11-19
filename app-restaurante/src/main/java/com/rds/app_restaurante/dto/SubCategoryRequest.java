package com.rds.app_restaurante.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubCategoryRequest {
    @NotBlank(message = "El nombre de la subcategoría es obligatorio")
    private String name;

    private String description;

    @NotNull(message = "El ID de la categoría es obligatorio")
    private Long categoryId;
}
