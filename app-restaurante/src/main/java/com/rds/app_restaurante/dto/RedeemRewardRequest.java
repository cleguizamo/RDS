package com.rds.app_restaurante.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RedeemRewardRequest {
    @NotNull(message = "El ID del producto de recompensa es obligatorio")
    private Long rewardProductId;
}

