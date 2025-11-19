package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.ProductRequest;
import com.rds.app_restaurante.dto.ProductResponse;
import com.rds.app_restaurante.model.Category;
import com.rds.app_restaurante.model.Product;
import com.rds.app_restaurante.model.SubCategory;
import com.rds.app_restaurante.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final SubCategoryService subCategoryService;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        Category category = categoryService.findById(categoryId);
        return productRepository.findByCategory(category).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
        return mapToResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        Category category = categoryService.findById(productRequest.getCategoryId());
        
        SubCategory subCategory = null;
        if (productRequest.getSubCategoryId() != null && productRequest.getSubCategoryId() > 0) {
            subCategory = subCategoryService.findById(productRequest.getSubCategoryId());
            // Validar que la subcategoría pertenezca a la categoría seleccionada
            if (!subCategory.getCategory().getId().equals(category.getId())) {
                throw new RuntimeException("La subcategoría seleccionada no pertenece a la categoría especificada");
            }
        }
        
        Product product;
        if (subCategory != null) {
            product = new Product(
                    productRequest.getName(),
                    productRequest.getDescription(),
                    productRequest.getImageUrl(),
                    productRequest.getPrice(),
                    category,
                    subCategory,
                    productRequest.getStock()
            );
        } else {
            product = new Product(
                    productRequest.getName(),
                    productRequest.getDescription(),
                    productRequest.getImageUrl(),
                    productRequest.getPrice(),
                    category,
                    productRequest.getStock()
            );
        }
        Product savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));

        Category category = categoryService.findById(productRequest.getCategoryId());

        SubCategory subCategory = null;
        if (productRequest.getSubCategoryId() != null && productRequest.getSubCategoryId() > 0) {
            subCategory = subCategoryService.findById(productRequest.getSubCategoryId());
            // Validar que la subcategoría pertenezca a la categoría seleccionada
            if (!subCategory.getCategory().getId().equals(category.getId())) {
                throw new RuntimeException("La subcategoría seleccionada no pertenece a la categoría especificada");
            }
        }

        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setImageUrl(productRequest.getImageUrl());
        product.setPrice(productRequest.getPrice());
        product.setCategory(category);
        product.setSubCategory(subCategory);
        product.setStock(productRequest.getStock());

        Product updatedProduct = productRepository.save(product);
        return mapToResponse(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Producto no encontrado con id: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .price(product.getPrice())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .subCategoryId(product.getSubCategory() != null ? product.getSubCategory().getId() : null)
                .subCategoryName(product.getSubCategory() != null ? product.getSubCategory().getName() : null)
                .stock(product.getStock())
                .build();
    }
}

