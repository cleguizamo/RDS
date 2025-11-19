package com.rds.app_restaurante.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private LocalDate date;
    private LocalTime time;
    private Double totalPrice;
    private Boolean status;
    private Integer tableNumber;
    private Long userId;
    private String userName;
    private String userEmail;
    private List<OrderItemResponse> items;
}

