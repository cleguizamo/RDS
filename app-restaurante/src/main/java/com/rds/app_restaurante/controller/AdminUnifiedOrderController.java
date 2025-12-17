package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.DeliveryResponse;
import com.rds.app_restaurante.dto.OrderResponse;
import com.rds.app_restaurante.service.DeliveryService;
import com.rds.app_restaurante.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/unified-orders")
@RequiredArgsConstructor

public class AdminUnifiedOrderController {

    private final OrderService orderService;
    private final DeliveryService deliveryService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllUnifiedOrders(
            @RequestParam(value = "type", required = false) String type) {
        List<Map<String, Object>> unifiedOrders = new ArrayList<>();

        // Obtener pedidos en mesa
        if (type == null || "EN_MESA".equals(type)) {
            List<OrderResponse> orders = orderService.getAllOrders();
            for (OrderResponse order : orders) {
                Map<String, Object> unified = new HashMap<>();
                unified.put("id", order.getId());
                unified.put("type", "EN_MESA");
                unified.put("date", order.getDate());
                unified.put("time", order.getTime());
                unified.put("totalPrice", order.getTotalPrice());
                unified.put("status", order.getStatus());
                unified.put("tableNumber", order.getTableNumber());
                unified.put("userId", order.getUserId());
                unified.put("userName", order.getUserName());
                unified.put("userEmail", order.getUserEmail());
                unified.put("items", order.getItems());
                // Campos de pago
                unified.put("paymentStatus", order.getPaymentStatus());
                unified.put("paymentMethod", order.getPaymentMethod());
                unified.put("paymentProofUrl", order.getPaymentProofUrl());
                unified.put("verifiedBy", order.getVerifiedBy());
                unified.put("verifiedByName", order.getVerifiedByName());
                unified.put("verifiedAt", order.getVerifiedAt());
                unifiedOrders.add(unified);
            }
        }

        // Obtener pedidos a domicilio
        if (type == null || "DOMICILIO".equals(type)) {
            List<DeliveryResponse> deliveries = deliveryService.getAllDeliveries();
            for (DeliveryResponse delivery : deliveries) {
                Map<String, Object> unified = new HashMap<>();
                unified.put("id", delivery.getId());
                unified.put("type", "DOMICILIO");
                unified.put("date", delivery.getDate());
                unified.put("time", delivery.getTime());
                unified.put("totalPrice", delivery.getTotalPrice());
                unified.put("status", delivery.getStatus());
                unified.put("deliveryAddress", delivery.getDeliveryAddress());
                unified.put("deliveryPhone", delivery.getDeliveryPhone());
                unified.put("userId", delivery.getUserId());
                unified.put("userName", delivery.getUserName());
                unified.put("userEmail", delivery.getUserEmail());
                unified.put("items", delivery.getItems());
                // Campos de pago
                unified.put("paymentStatus", delivery.getPaymentStatus());
                unified.put("paymentMethod", delivery.getPaymentMethod());
                unified.put("paymentProofUrl", delivery.getPaymentProofUrl());
                unified.put("verifiedBy", delivery.getVerifiedByAdminId());
                unified.put("verifiedByName", delivery.getVerifiedByAdminName());
                unified.put("verifiedAt", delivery.getVerifiedAt());
                unifiedOrders.add(unified);
            }
        }

        return ResponseEntity.ok(unifiedOrders);
    }
}

