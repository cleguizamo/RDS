package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.ProductRequest;
import com.rds.app_restaurante.dto.ProductResponse;
import com.rds.app_restaurante.model.Category;
import com.rds.app_restaurante.model.Product;
import com.rds.app_restaurante.model.SubCategory;
import com.rds.app_restaurante.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.rds.app_restaurante.dto.ProductSearchRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final SubCategoryService subCategoryService;

    @Transactional(readOnly = true)
    @Cacheable(value = "products", unless = "#result.isEmpty()")
    public List<ProductResponse> getAllProducts() {
        log.debug("Fetching all products from database");
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProductsPaginated(Pageable pageable) {
        log.debug("Fetching products page: {} with size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return productRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(ProductSearchRequest searchRequest) {
        log.debug("Searching products with filters: {}", searchRequest);
        
        // Configurar paginación y ordenamiento
        int page = searchRequest.getPage() != null ? searchRequest.getPage() : 0;
        int size = searchRequest.getSize() != null ? searchRequest.getSize() : 20;
        
        Sort sort = Sort.unsorted();
        if (searchRequest.getSortBy() != null && !searchRequest.getSortBy().isEmpty()) {
            Sort.Direction direction = Sort.Direction.ASC;
            if (searchRequest.getSortDirection() != null && 
                searchRequest.getSortDirection().equalsIgnoreCase("DESC")) {
                direction = Sort.Direction.DESC;
            }
            sort = Sort.by(direction, searchRequest.getSortBy());
        }
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Ejecutar búsqueda con filtros
        Page<Product> products = productRepository.searchProducts(
                searchRequest.getName(),
                searchRequest.getCategoryId(),
                searchRequest.getSubCategoryId(),
                searchRequest.getMinPrice(),
                searchRequest.getMaxPrice(),
                searchRequest.getMinStock(),
                pageable
        );
        
        log.debug("Found {} products matching search criteria", products.getTotalElements());
        return products.map(this::mapToResponse);
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
    @CacheEvict(value = {"products", "statistics"}, allEntries = true)
    public ProductResponse createProduct(ProductRequest productRequest) {
        log.info("Creating new product: {}", productRequest.getName());
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
            log.info("Product created successfully with ID: {}", savedProduct.getId());
            return mapToResponse(savedProduct);
        }

    @Transactional
    @CacheEvict(value = {"products", "statistics"}, allEntries = true)
    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        log.info("Updating product with ID: {}", id);
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
            log.info("Product updated successfully with ID: {}", updatedProduct.getId());
            return mapToResponse(updatedProduct);
        }

    @Transactional
    @CacheEvict(value = {"products", "statistics"}, allEntries = true)
    public void deleteProduct(Long id) {
        log.info("Attempting to delete product with ID: {}", id);
        if (!productRepository.existsById(id)) {
            log.warn("Attempt to delete non-existent product with ID: {}", id);
            throw new RuntimeException("Producto no encontrado con id: " + id);
        }
        productRepository.deleteById(id);
        log.info("Product deleted successfully with ID: {}", id);
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

