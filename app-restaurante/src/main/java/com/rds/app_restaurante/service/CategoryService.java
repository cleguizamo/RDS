package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.CategoryRequest;
import com.rds.app_restaurante.dto.CategoryResponse;
import com.rds.app_restaurante.model.Category;
import com.rds.app_restaurante.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
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
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {
        // Verificar si ya existe una categoría con el mismo nombre
        if (categoryRepository.existsByName(categoryRequest.getName())) {
            throw new RuntimeException("Ya existe una categoría con el nombre: " + categoryRequest.getName());
        }

        Category category = new Category(
                categoryRequest.getName(),
                categoryRequest.getDescription()
        );
        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + id));

        // Verificar si el nuevo nombre ya existe en otra categoría
        if (!category.getName().equals(categoryRequest.getName()) && 
            categoryRepository.existsByName(categoryRequest.getName())) {
            throw new RuntimeException("Ya existe una categoría con el nombre: " + categoryRequest.getName());
        }

        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());

        Category updatedCategory = categoryRepository.save(category);
        return mapToResponse(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + id));

        // Verificar si la categoría tiene productos asociados
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new RuntimeException("No se puede eliminar la categoría porque tiene productos asociados. " +
                    "Primero elimine o reasigne los productos de esta categoría.");
        }

        categoryRepository.deleteById(id);
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
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .productCount(category.getProducts() != null ? category.getProducts().size() : 0)
                .build();
    }
}

