package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.SubCategoryRequest;
import com.rds.app_restaurante.dto.SubCategoryResponse;
import com.rds.app_restaurante.model.Category;
import com.rds.app_restaurante.model.SubCategory;
import com.rds.app_restaurante.repository.SubCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryService categoryService;

    @Transactional(readOnly = true)
    public List<SubCategoryResponse> getAllSubCategories() {
        return subCategoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubCategoryResponse> getSubCategoriesByCategory(Long categoryId) {
        return subCategoryRepository.findByCategoryId(categoryId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubCategoryResponse getSubCategoryById(Long id) {
        SubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subcategoría no encontrada con id: " + id));
        return mapToResponse(subCategory);
    }

    @Transactional
    public SubCategoryResponse createSubCategory(SubCategoryRequest subCategoryRequest) {
        Category category = categoryService.findById(subCategoryRequest.getCategoryId());

        // Verificar si ya existe una subcategoría con el mismo nombre en la misma categoría
        if (subCategoryRepository.existsByNameAndCategory(subCategoryRequest.getName(), category)) {
            throw new RuntimeException("Ya existe una subcategoría con el nombre '" + 
                    subCategoryRequest.getName() + "' en la categoría '" + category.getName() + "'");
        }

        SubCategory subCategory = new SubCategory(
                subCategoryRequest.getName(),
                subCategoryRequest.getDescription(),
                category
        );
        SubCategory savedSubCategory = subCategoryRepository.save(subCategory);
        return mapToResponse(savedSubCategory);
    }

    @Transactional
    public SubCategoryResponse updateSubCategory(Long id, SubCategoryRequest subCategoryRequest) {
        SubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subcategoría no encontrada con id: " + id));

        Category category = categoryService.findById(subCategoryRequest.getCategoryId());

        // Verificar si el nuevo nombre ya existe en otra subcategoría de la misma categoría
        if (!subCategory.getName().equals(subCategoryRequest.getName()) && 
            subCategoryRepository.existsByNameAndCategory(subCategoryRequest.getName(), category)) {
            throw new RuntimeException("Ya existe una subcategoría con el nombre '" + 
                    subCategoryRequest.getName() + "' en la categoría '" + category.getName() + "'");
        }

        subCategory.setName(subCategoryRequest.getName());
        subCategory.setDescription(subCategoryRequest.getDescription());
        subCategory.setCategory(category);

        SubCategory updatedSubCategory = subCategoryRepository.save(subCategory);
        return mapToResponse(updatedSubCategory);
    }

    @Transactional
    public void deleteSubCategory(Long id) {
        SubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subcategoría no encontrada con id: " + id));

        // Verificar si la subcategoría tiene productos asociados
        if (subCategory.getProducts() != null && !subCategory.getProducts().isEmpty()) {
            throw new RuntimeException("No se puede eliminar la subcategoría porque tiene productos asociados. " +
                    "Primero elimine o reasigne los productos de esta subcategoría.");
        }

        subCategoryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public SubCategory findById(Long id) {
        return subCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subcategoría no encontrada con id: " + id));
    }

    private SubCategoryResponse mapToResponse(SubCategory subCategory) {
        return SubCategoryResponse.builder()
                .id(subCategory.getId())
                .name(subCategory.getName())
                .description(subCategory.getDescription())
                .categoryId(subCategory.getCategory() != null ? subCategory.getCategory().getId() : null)
                .categoryName(subCategory.getCategory() != null ? subCategory.getCategory().getName() : null)
                .productCount(subCategory.getProducts() != null ? subCategory.getProducts().size() : 0)
                .build();
    }
}
