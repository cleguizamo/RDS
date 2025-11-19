package com.rds.app_restaurante.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "reward_products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "points_required", nullable = false)
    private Long pointsRequired;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    public RewardProduct(String name, String description, String imageUrl, Long pointsRequired, Integer stock, Boolean isActive) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.pointsRequired = pointsRequired;
        this.stock = stock;
        this.isActive = isActive;
    }
}

