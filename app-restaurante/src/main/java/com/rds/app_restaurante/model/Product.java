package com.rds.app_restaurante.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "ImageUrl", nullable = false)
    private String imageUrl;

    @Column(name = "price", nullable = false)
    private double price;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subcategory_id", nullable = true)
    private SubCategory subCategory;

    @Column(name = "stock", nullable = false)
    private int stock;

    //Constructor para crear un nuevo producto (Sin ID ya que lo genera la base de datos)
    public Product(String name, String description, String imageUrl, double price, Category category, int stock) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.price = price;
        this.category = category;
        this.stock = stock;
    }

    //Constructor con subcategoría
    public Product(String name, String description, String imageUrl, double price, Category category, SubCategory subCategory, int stock) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.price = price;
        this.category = category;
        this.subCategory = subCategory;
        this.stock = stock;
    }
}
