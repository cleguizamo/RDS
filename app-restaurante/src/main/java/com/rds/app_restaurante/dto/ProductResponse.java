package com.rds.app_restaurante.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Double price;
    private Long categoryId;
    private String categoryName;
    private Long subCategoryId;
    private String subCategoryName;
    private Integer stock;
}

