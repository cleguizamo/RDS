package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.Security.JwtUtil;
import com.rds.app_restaurante.dto.RedeemRewardRequest;
import com.rds.app_restaurante.dto.RewardProductResponse;
import com.rds.app_restaurante.service.RewardProductService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/client/rewards")
@RequiredArgsConstructor

public class ClientRewardController {

    private final RewardProductService rewardProductService;
    private final JwtUtil jwtUtil;

    @PostMapping("/redeem")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> redeemReward(
            @Valid @RequestBody RedeemRewardRequest redeemRequest,
            HttpServletRequest request) {
        try {
            // Extraer userId del token JWT
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token no válido"));
            }

            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);

            RewardProductResponse redeemedReward = rewardProductService.redeemReward(userId, redeemRequest.getRewardProductId());
            return ResponseEntity.ok(Map.of(
                    "message", "Recompensa canjeada exitosamente",
                    "reward", redeemedReward
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al canjear la recompensa"));
        }
    }
}

