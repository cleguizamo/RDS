package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.RewardProductRequest;
import com.rds.app_restaurante.dto.RewardProductResponse;
import com.rds.app_restaurante.service.RewardProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/rewards")
@RequiredArgsConstructor

public class RewardProductController {

    private final RewardProductService rewardProductService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RewardProductResponse>> getAllRewardProducts() {
        List<RewardProductResponse> rewards = rewardProductService.getAllRewardProducts();
        return ResponseEntity.ok(rewards);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRewardProductById(@PathVariable Long id) {
        try {
            RewardProductResponse reward = rewardProductService.getRewardProductById(id);
            return ResponseEntity.ok(reward);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createRewardProduct(@Valid @RequestBody RewardProductRequest rewardProductRequest) {
        try {
            RewardProductResponse reward = rewardProductService.createRewardProduct(rewardProductRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(reward);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al crear el producto de recompensa"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateRewardProduct(@PathVariable Long id, @Valid @RequestBody RewardProductRequest rewardProductRequest) {
        try {
            RewardProductResponse reward = rewardProductService.updateRewardProduct(id, rewardProductRequest);
            return ResponseEntity.ok(reward);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al actualizar el producto de recompensa"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteRewardProduct(@PathVariable Long id) {
        try {
            rewardProductService.deleteRewardProduct(id);
            return ResponseEntity.ok(Map.of("message", "Producto de recompensa eliminado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al eliminar el producto de recompensa"));
        }
    }
}

