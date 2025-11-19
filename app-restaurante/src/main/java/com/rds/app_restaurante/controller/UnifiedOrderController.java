package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.DeliveryResponse;
import com.rds.app_restaurante.dto.OrderResponse;
import com.rds.app_restaurante.service.DeliveryService;
import com.rds.app_restaurante.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employee/unified-orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UnifiedOrderController {

    private final OrderService orderService;
    private final DeliveryService deliveryService;

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<Map<String, Object>>> getAllUnifiedOrders(
            @RequestParam(required = false) String type) {
        
        System.out.println("=== getAllUnifiedOrders llamado ===");
        System.out.println("Type filter: " + type);
        
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Authentication: " + authentication);
        System.out.println("Authentication is null: " + (authentication == null));
        System.out.println("Authentication is authenticated: " + (authentication != null && authentication.isAuthenticated()));
        if (authentication != null) {
            System.out.println("Authentication authorities: " + authentication.getAuthorities());
            System.out.println("Authentication name: " + authentication.getName());
        }
        
        // Verificar autorización manualmente para mejor logging
        if (authentication == null || !authentication.isAuthenticated()) {
            System.err.println("Error: No hay autenticación establecida");
            return ResponseEntity.status(403).build();
        }
        
        boolean hasEmployeeRole = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));
        
        if (!hasEmployeeRole) {
            System.err.println("Error: El usuario no tiene el rol EMPLOYEE. Roles: " + authentication.getAuthorities());
            return ResponseEntity.status(403).build();
        }
        
        System.out.println("Autorización exitosa - usuario tiene rol EMPLOYEE");
        
        List<Map<String, Object>> unifiedOrders = new ArrayList<>();

        // Obtener pedidos en mesa
        if (type == null || "EN_MESA".equals(type)) {
            System.out.println("Obteniendo pedidos en mesa...");
            List<OrderResponse> orders = orderService.getAllOrders();
            System.out.println("Pedidos en mesa encontrados: " + orders.size());
            for (OrderResponse order : orders) {
                Map<String, Object> unified = new HashMap<>();
                unified.put("id", order.getId());
                unified.put("type", "EN_MESA"); // Asegurar que el tipo sea EN_MESA
                unified.put("date", order.getDate());
                unified.put("time", order.getTime());
                unified.put("totalPrice", order.getTotalPrice());
                unified.put("status", order.getStatus());
                unified.put("tableNumber", order.getTableNumber());
                unified.put("userId", order.getUserId());
                unified.put("userName", order.getUserName());
                unified.put("userEmail", order.getUserEmail());
                unified.put("items", order.getItems() != null ? order.getItems() : new ArrayList<>());
                // NO incluir campos de delivery para orders
                unifiedOrders.add(unified);
                System.out.println("Pedido EN_MESA agregado: ID=" + order.getId() + ", Type=" + unified.get("type"));
            }
        }

        // Obtener pedidos a domicilio
        if (type == null || "DOMICILIO".equals(type)) {
            System.out.println("Obteniendo pedidos a domicilio...");
            List<DeliveryResponse> deliveries = deliveryService.getAllDeliveries();
            System.out.println("Pedidos a domicilio encontrados: " + deliveries.size());
            for (DeliveryResponse delivery : deliveries) {
                Map<String, Object> unified = new HashMap<>();
                unified.put("id", delivery.getId());
                unified.put("type", "DOMICILIO"); // Asegurar que el tipo sea DOMICILIO
                unified.put("date", delivery.getDate());
                unified.put("time", delivery.getTime());
                unified.put("totalPrice", delivery.getTotalPrice());
                unified.put("status", delivery.getStatus());
                unified.put("deliveryAddress", delivery.getDeliveryAddress());
                unified.put("deliveryPhone", delivery.getDeliveryPhone());
                unified.put("userId", delivery.getUserId());
                unified.put("userName", delivery.getUserName());
                unified.put("userEmail", delivery.getUserEmail());
                unified.put("items", delivery.getItems() != null ? delivery.getItems() : new ArrayList<>());
                // NO incluir tableNumber para deliveries
                unifiedOrders.add(unified);
                System.out.println("Pedido DOMICILIO agregado: ID=" + delivery.getId() + ", Type=" + unified.get("type"));
            }
        }

        System.out.println("Total de pedidos unificados: " + unifiedOrders.size());
        return ResponseEntity.ok(unifiedOrders);
    }
}

