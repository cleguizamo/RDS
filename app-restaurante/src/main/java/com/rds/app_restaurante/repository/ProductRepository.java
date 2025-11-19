package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.Category;
import com.rds.app_restaurante.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(Category category);
    Optional<Product> findByName(String name);
}

