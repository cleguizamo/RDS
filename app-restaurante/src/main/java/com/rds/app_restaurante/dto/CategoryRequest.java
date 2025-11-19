package com.rds.app_restaurante.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank(message = "El nombre de la categoría es obligatorio")
    private String name;

    private String description;
}

