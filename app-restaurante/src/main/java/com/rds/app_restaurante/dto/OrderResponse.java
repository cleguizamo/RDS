package com.rds.app_restaurante.dto;

import com.rds.app_restaurante.model.OrderType;
import com.rds.app_restaurante.model.PaymentMethod;
import com.rds.app_restaurante.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
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
    private OrderType type;
    private Integer tableNumber;
    private Long userId;
    private String userName;
    private String userEmail;
    private List<OrderItemResponse> items;
    
    // Campos de pago
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private String paymentProofUrl;
    private Long verifiedBy;
    private String verifiedByName;
    private LocalDateTime verifiedAt;
}

