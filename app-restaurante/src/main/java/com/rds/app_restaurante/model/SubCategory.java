package com.rds.app_restaurante.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subcategories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "subCategory", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Product> products = new ArrayList<>();

    public SubCategory(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }
}
