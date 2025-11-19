package com.rds.app_restaurante.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubCategoryResponse {
    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Integer productCount;
}
