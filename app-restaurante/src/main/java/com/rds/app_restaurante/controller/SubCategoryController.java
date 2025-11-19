package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.SubCategoryRequest;
import com.rds.app_restaurante.dto.SubCategoryResponse;
import com.rds.app_restaurante.service.SubCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/subcategories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubCategoryController {

    private final SubCategoryService subCategoryService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubCategoryResponse>> getAllSubCategories() {
        List<SubCategoryResponse> subCategories = subCategoryService.getAllSubCategories();
        return ResponseEntity.ok(subCategories);
    }

    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubCategoryResponse>> getSubCategoriesByCategory(@PathVariable Long categoryId) {
        List<SubCategoryResponse> subCategories = subCategoryService.getSubCategoriesByCategory(categoryId);
        return ResponseEntity.ok(subCategories);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSubCategoryById(@PathVariable Long id) {
        try {
            SubCategoryResponse subCategory = subCategoryService.getSubCategoryById(id);
            return ResponseEntity.ok(subCategory);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createSubCategory(@Valid @RequestBody SubCategoryRequest subCategoryRequest) {
        try {
            SubCategoryResponse subCategory = subCategoryService.createSubCategory(subCategoryRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(subCategory);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al crear la subcategoría"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSubCategory(@PathVariable Long id, @Valid @RequestBody SubCategoryRequest subCategoryRequest) {
        try {
            SubCategoryResponse subCategory = subCategoryService.updateSubCategory(id, subCategoryRequest);
            return ResponseEntity.ok(subCategory);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al actualizar la subcategoría"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSubCategory(@PathVariable Long id) {
        try {
            subCategoryService.deleteSubCategory(id);
            return ResponseEntity.ok(Map.of("message", "Subcategoría eliminada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al eliminar la subcategoría"));
        }
    }
}
