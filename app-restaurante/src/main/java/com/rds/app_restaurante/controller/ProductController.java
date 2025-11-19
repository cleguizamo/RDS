package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.ProductRequest;
import com.rds.app_restaurante.dto.ProductResponse;
import com.rds.app_restaurante.service.CloudinaryService;
import com.rds.app_restaurante.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final CloudinaryService cloudinaryService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            ProductResponse product = productService.getProductById(id);
            return ResponseEntity.ok(product);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getProductsByCategory(@PathVariable Long categoryId) {
        try {
            List<ProductResponse> products = productService.getProductsByCategory(categoryId);
            return ResponseEntity.ok(products);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createProduct(@Valid @RequestBody ProductRequest productRequest) {
        try {
            ProductResponse product = productService.createProduct(productRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(product);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear el producto");
        }
    }

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("=== Iniciando subida de imagen ===");
            System.out.println("File recibido: " + (file != null ? "Sí" : "No"));
            
            if (file == null || file.isEmpty()) {
                System.out.println("Error: Archivo vacío o nulo");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "El archivo está vacío"));
            }

            System.out.println("Nombre archivo: " + file.getOriginalFilename());
            System.out.println("Tamaño archivo: " + file.getSize() + " bytes");
            System.out.println("Content-Type: " + file.getContentType());

            // Validar tipo de archivo
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                System.out.println("Error: Tipo de archivo inválido: " + contentType);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "El archivo debe ser una imagen"));
            }

            System.out.println("Subiendo a Cloudinary...");
            Map<String, Object> uploadResult = cloudinaryService.upload(file);
            System.out.println("Resultado Cloudinary recibido: " + (uploadResult != null ? "Sí" : "No"));
            
            if (uploadResult == null || uploadResult.isEmpty()) {
                System.out.println("Error: Cloudinary devolvió un resultado vacío o nulo");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Error al subir la imagen: Cloudinary no devolvió un resultado válido"));
            }
            
            // Intentar obtener secure_url primero, si no existe usar url
            String imageUrl = (String) uploadResult.get("secure_url");
            if (imageUrl == null) {
                imageUrl = (String) uploadResult.get("url");
            }
            
            if (imageUrl == null) {
                System.out.println("Error: No se encontró URL en la respuesta de Cloudinary");
                System.out.println("Claves disponibles: " + uploadResult.keySet());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Error al obtener la URL de la imagen subida"));
            }
            
            System.out.println("Imagen subida exitosamente. URL: " + imageUrl);
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (IOException e) {
            System.err.println("IOException al subir imagen: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al subir la imagen: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("Exception inesperada al subir imagen: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error inesperado al subir la imagen: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest productRequest) {
        try {
            ProductResponse product = productService.updateProduct(id, productRequest);
            return ResponseEntity.ok(product);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar el producto");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(Map.of("message", "Producto eliminado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar el producto");
        }
    }
}

