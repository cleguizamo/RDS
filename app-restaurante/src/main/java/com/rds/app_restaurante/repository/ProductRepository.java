package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.Category;
import com.rds.app_restaurante.model.Product;
import com.rds.app_restaurante.model.SubCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findByCategory(Category category);
    Optional<Product> findByName(String name);
    
    // Búsqueda por nombre (case-insensitive)
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    // Búsqueda por categoría
    Page<Product> findByCategory(Category category, Pageable pageable);
    
    // Búsqueda por subcategoría
    Page<Product> findBySubCategory(SubCategory subCategory, Pageable pageable);
    
    // Búsqueda por rango de precio
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceBetween(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice, Pageable pageable);
    
    // Búsqueda por stock disponible
    Page<Product> findByStockGreaterThan(Integer stock, Pageable pageable);
    
    // Búsqueda combinada: nombre y categoría
    Page<Product> findByNameContainingIgnoreCaseAndCategory(String name, Category category, Pageable pageable);
    
    // Búsqueda combinada: nombre, categoría y rango de precio
    @Query("SELECT p FROM Product p WHERE " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:subCategoryId IS NULL OR p.subCategory.id = :subCategoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:minStock IS NULL OR p.stock >= :minStock)")
    Page<Product> searchProducts(
            @Param("name") String name,
            @Param("categoryId") Long categoryId,
            @Param("subCategoryId") Long subCategoryId,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("minStock") Integer minStock,
            Pageable pageable
    );
}
