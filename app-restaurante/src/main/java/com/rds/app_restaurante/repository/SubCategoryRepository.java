package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.Category;
import com.rds.app_restaurante.model.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {
    List<SubCategory> findByCategory(Category category);
    List<SubCategory> findByCategoryId(Long categoryId);
    Optional<SubCategory> findByNameAndCategory(String name, Category category);
    boolean existsByNameAndCategory(String name, Category category);
}
