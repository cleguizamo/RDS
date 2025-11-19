package com.rds.app_restaurante.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardProductResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Long pointsRequired;
    private Integer stock;
    private Boolean isActive;
}

