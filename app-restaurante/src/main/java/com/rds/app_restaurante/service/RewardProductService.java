package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.RewardProductRequest;
import com.rds.app_restaurante.dto.RewardProductResponse;
import com.rds.app_restaurante.model.RewardProduct;
import com.rds.app_restaurante.model.User;
import com.rds.app_restaurante.repository.RewardProductRepository;
import com.rds.app_restaurante.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RewardProductService {

    private final RewardProductRepository rewardProductRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<RewardProductResponse> getAllRewardProducts() {
        return rewardProductRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RewardProductResponse> getActiveRewardProducts() {
        return rewardProductRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RewardProductResponse getRewardProductById(Long id) {
        RewardProduct rewardProduct = rewardProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto de recompensa no encontrado con id: " + id));
        return mapToResponse(rewardProduct);
    }

    @Transactional
    public RewardProductResponse createRewardProduct(RewardProductRequest rewardProductRequest) {
        RewardProduct rewardProduct = new RewardProduct(
                rewardProductRequest.getName(),
                rewardProductRequest.getDescription(),
                rewardProductRequest.getImageUrl(),
                rewardProductRequest.getPointsRequired(),
                rewardProductRequest.getStock(),
                rewardProductRequest.getIsActive()
        );
        RewardProduct savedRewardProduct = rewardProductRepository.save(rewardProduct);
        return mapToResponse(savedRewardProduct);
    }

    @Transactional
    public RewardProductResponse updateRewardProduct(Long id, RewardProductRequest rewardProductRequest) {
        RewardProduct rewardProduct = rewardProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto de recompensa no encontrado con id: " + id));

        rewardProduct.setName(rewardProductRequest.getName());
        rewardProduct.setDescription(rewardProductRequest.getDescription());
        rewardProduct.setImageUrl(rewardProductRequest.getImageUrl());
        rewardProduct.setPointsRequired(rewardProductRequest.getPointsRequired());
        rewardProduct.setStock(rewardProductRequest.getStock());
        rewardProduct.setIsActive(rewardProductRequest.getIsActive());

        RewardProduct updatedRewardProduct = rewardProductRepository.save(rewardProduct);
        return mapToResponse(updatedRewardProduct);
    }

    @Transactional
    public void deleteRewardProduct(Long id) {
        if (!rewardProductRepository.existsById(id)) {
            throw new RuntimeException("Producto de recompensa no encontrado con id: " + id);
        }
        rewardProductRepository.deleteById(id);
    }

    @Transactional
    public RewardProductResponse redeemReward(Long userId, Long rewardProductId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));

        RewardProduct rewardProduct = rewardProductRepository.findById(rewardProductId)
                .orElseThrow(() -> new RuntimeException("Producto de recompensa no encontrado con id: " + rewardProductId));

        // Validar que el producto esté activo
        if (!rewardProduct.getIsActive()) {
            throw new RuntimeException("El producto de recompensa no está disponible");
        }

        // Validar stock
        if (rewardProduct.getStock() <= 0) {
            throw new RuntimeException("No hay stock disponible para este producto de recompensa");
        }

        // Validar puntos del usuario
        if (user.getPoints() < rewardProduct.getPointsRequired()) {
            throw new RuntimeException("No tienes suficientes puntos. Necesitas " + 
                    rewardProduct.getPointsRequired() + " puntos y tienes " + user.getPoints());
        }

        // Reducir puntos del usuario
        user.setPoints(user.getPoints() - rewardProduct.getPointsRequired());
        
        // Reducir stock del producto de recompensa
        rewardProduct.setStock(rewardProduct.getStock() - 1);

        userRepository.save(user);
        rewardProductRepository.save(rewardProduct);

        return mapToResponse(rewardProduct);
    }

    private RewardProductResponse mapToResponse(RewardProduct rewardProduct) {
        return RewardProductResponse.builder()
                .id(rewardProduct.getId())
                .name(rewardProduct.getName())
                .description(rewardProduct.getDescription())
                .imageUrl(rewardProduct.getImageUrl())
                .pointsRequired(rewardProduct.getPointsRequired())
                .stock(rewardProduct.getStock())
                .isActive(rewardProduct.getIsActive())
                .build();
    }
}

