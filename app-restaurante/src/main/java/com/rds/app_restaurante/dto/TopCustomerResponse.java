package com.rds.app_restaurante.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopCustomerResponse {
    private Long userId;
    private String name;
    private String lastName;
    private Long totalOrders;
    private BigDecimal totalSpent;
}

