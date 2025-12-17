package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.CategoryRequest;
import com.rds.app_restaurante.dto.CategoryResponse;
import com.rds.app_restaurante.model.Category;
import com.rds.app_restaurante.repository.CategoryRepository;
import com.rds.app_restaurante.repository.ProductRepository;
import com.rds.app_restaurante.repository.SubCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SubCategoryRepository subCategoryRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "categories", unless = "#result.isEmpty()")
    public List<CategoryResponse> getAllCategories() {
        log.debug("Fetching all categories from database");
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + id));
        return mapToResponse(category);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {
        log.info("Creating new category: {}", categoryRequest.getName());
        // Verificar si ya existe una categoría con el mismo nombre
        if (categoryRepository.existsByName(categoryRequest.getName())) {
            log.warn("Attempt to create duplicate category: {}", categoryRequest.getName());
            throw new RuntimeException("Ya existe una categoría con el nombre: " + categoryRequest.getName());
        }

        Category category = new Category(
                categoryRequest.getName(),
                categoryRequest.getDescription()
        );
        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully with ID: {}", savedCategory.getId());
        return mapToResponse(savedCategory);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest) {
        log.info("Updating category with ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + id));

        // Verificar si el nuevo nombre ya existe en otra categoría
        if (!category.getName().equals(categoryRequest.getName()) && 
            categoryRepository.existsByName(categoryRequest.getName())) {
            log.warn("Attempt to update category with duplicate name: {}", categoryRequest.getName());
            throw new RuntimeException("Ya existe una categoría con el nombre: " + categoryRequest.getName());
        }

        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated successfully with ID: {}", updatedCategory.getId());
        return mapToResponse(updatedCategory);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        log.info("Attempting to delete category with ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + id));

        // Verificar si hay productos asociados
        if (!productRepository.findByCategory(category).isEmpty()) {
            log.warn("Cannot delete category {}: has associated products", id);
            throw new RuntimeException("No se puede eliminar la categoría porque tiene productos asociados.");
        }
        // Verificar si hay subcategorías asociadas
        if (!subCategoryRepository.findByCategory(category).isEmpty()) {
            log.warn("Cannot delete category {}: has associated subcategories", id);
            throw new RuntimeException("No se puede eliminar la categoría porque tiene subcategorías asociadas.");
        }

        categoryRepository.delete(category);
        log.info("Category deleted successfully with ID: {}", id);
    }

    @Transactional(readOnly = true)
    public Category findByName(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con el nombre: " + name));
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + id));
    }

    private CategoryResponse mapToResponse(Category category) {
        int productCount = productRepository.findByCategory(category).size();
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .productCount(productCount)
                .build();
    }
}

