package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.RewardProductResponse;
import com.rds.app_restaurante.service.RewardProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/rewards")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicRewardProductController {

    private final RewardProductService rewardProductService;

    @GetMapping
    public ResponseEntity<List<RewardProductResponse>> getActiveRewardProducts() {
        List<RewardProductResponse> rewards = rewardProductService.getActiveRewardProducts();
        return ResponseEntity.ok(rewards);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRewardProductById(@PathVariable Long id) {
        try {
            RewardProductResponse reward = rewardProductService.getRewardProductById(id);
            return ResponseEntity.ok(reward);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

