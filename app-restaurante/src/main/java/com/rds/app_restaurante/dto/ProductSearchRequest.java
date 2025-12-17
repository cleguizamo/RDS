package com.rds.app_restaurante.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchRequest {
    private String name;
    private Long categoryId;
    private Long subCategoryId;
    private Double minPrice;
    private Double maxPrice;
    private Integer minStock;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection; // ASC, DESC
}

