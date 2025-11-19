package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.SubCategoryResponse;
import com.rds.app_restaurante.service.SubCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/subcategories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicSubCategoryController {

    private final SubCategoryService subCategoryService;

    @GetMapping
    public ResponseEntity<List<SubCategoryResponse>> getAllSubCategories() {
        List<SubCategoryResponse> subCategories = subCategoryService.getAllSubCategories();
        return ResponseEntity.ok(subCategories);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<SubCategoryResponse>> getSubCategoriesByCategory(@PathVariable Long categoryId) {
        List<SubCategoryResponse> subCategories = subCategoryService.getSubCategoriesByCategory(categoryId);
        return ResponseEntity.ok(subCategories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSubCategoryById(@PathVariable Long id) {
        try {
            SubCategoryResponse subCategory = subCategoryService.getSubCategoryById(id);
            return ResponseEntity.ok(subCategory);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
