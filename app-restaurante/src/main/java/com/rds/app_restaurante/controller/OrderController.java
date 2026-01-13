package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.Security.JwtUtil;
import com.rds.app_restaurante.dto.OrderRequest;
import com.rds.app_restaurante.dto.OrderResponse;
import com.rds.app_restaurante.service.CloudinaryService;
import com.rds.app_restaurante.service.OrderService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor

public class OrderController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;
    private final CloudinaryService cloudinaryService;

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    // Rutas específicas primero para evitar conflictos con /{id}
    @GetMapping("/admin/orders/pending-payments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getOrdersWithPendingPayments() {
        List<OrderResponse> orders = orderService.getOrdersWithPendingPayments();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/admin/orders/verified-payments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getOrdersWithVerifiedPayments() {
        List<OrderResponse> orders = orderService.getOrdersWithVerifiedPayments();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/admin/orders/date/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getOrdersByDate(
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<OrderResponse> orders = orderService.getOrdersByDate(date);
        return ResponseEntity.ok(orders);
    }

    // Endpoint para administradores confirmar pedidos
    @PutMapping("/admin/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateOrderStatusByAdmin(@PathVariable("id") Long id, @RequestBody Map<String, Boolean> request) {
        System.out.println("=== Endpoint updateOrderStatusByAdmin llamado con ID: " + id + " ===");
        try {
            Boolean status = request.get("status");
            if (status == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El campo 'status' es obligatorio");
            }
            OrderResponse order = orderService.updateOrderStatus(id, status);
            System.out.println("=== Pedido actualizado exitosamente: " + order.getId() + " ===");
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            System.err.println("=== Error al actualizar pedido: " + e.getMessage() + " ===");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("=== Error inesperado al actualizar pedido: " + e.getMessage() + " ===");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar el estado de la orden");
        }
    }

    @GetMapping("/admin/orders/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getOrderById(@PathVariable("id") Long id) {
        try {
            OrderResponse order = orderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/employee/orders")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<OrderResponse>> getAllOrdersForEmployee() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/employee/orders/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> getOrderByIdForEmployee(@PathVariable("id") Long id) {
        try {
            OrderResponse order = orderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/employee/orders")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        try {
            OrderResponse order = orderService.createOrder(orderRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear la orden");
        }
    }

    @PutMapping("/employee/orders/{id}/status")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> updateOrderStatus(@PathVariable("id") Long id, @RequestBody Map<String, Boolean> request) {
        try {
            Boolean status = request.get("status");
            if (status == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El campo 'status' es obligatorio");
            }
            OrderResponse order = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar el estado de la orden");
        }
    }

    // Endpoints para clientes
    @PostMapping("/client/orders")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> createClientOrder(@Valid @RequestBody OrderRequest orderRequest) {
        try {
            OrderResponse order = orderService.createOrder(orderRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear la orden");
        }
    }

    @GetMapping("/client/orders")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<OrderResponse>> getClientOrders(
            HttpServletRequest request,
            @RequestParam(value = "userId", required = false) Long userId) {
        try {
            Long clientUserId = userId;
            
            // Si no se proporciona userId, obtenerlo del token JWT
            if (clientUserId == null) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    try {
                        clientUserId = jwtUtil.extractUserId(token);
                    } catch (Exception e) {
                        // Si no se puede extraer del token, intentar desde el parámetro
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Collections.emptyList());
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Collections.emptyList());
                }
            }
            
            if (clientUserId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.emptyList());
            }
            
            List<OrderResponse> orders = orderService.getOrdersByUserId(clientUserId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    @GetMapping("/client/orders/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getClientOrderById(@PathVariable("id") Long id) {
        try {
            OrderResponse order = orderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Endpoints para pagos (verificar y rechazar)
    @PostMapping("/admin/orders/{id}/verify-payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> verifyPayment(
            @PathVariable("id") Long orderId,
            HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token de autenticación requerido"));
            }
            
            String token = authHeader.substring(7);
            Long adminId = jwtUtil.extractUserId(token);
            
            OrderResponse order = orderService.verifyPayment(orderId, adminId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al verificar el pago"));
        }
    }

    @PostMapping("/admin/orders/{id}/reject-payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectPayment(
            @PathVariable("id") Long orderId,
            @RequestBody(required = false) Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token de autenticación requerido"));
            }
            
            String token = authHeader.substring(7);
            Long adminId = jwtUtil.extractUserId(token);
            
            String reason = request != null ? request.get("reason") : null;
            OrderResponse order = orderService.rejectPayment(orderId, adminId, reason);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al rechazar el pago"));
        }
    }

    @PostMapping("/client/orders/{id}/upload-payment-proof")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> uploadPaymentProof(
            @PathVariable("id") Long orderId,
            @RequestParam("file") MultipartFile file) {
        try {
            // Subir imagen a Cloudinary
            Map<String, Object> uploadResult = cloudinaryService.upload(file, "restaurante/comprobantes-pago");
            String imageUrl = (String) uploadResult.get("url");
            
            if (imageUrl == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Error al subir la imagen"));
            }
            
            // Actualizar el pedido con la URL del comprobante
            OrderResponse order = orderService.updatePaymentProof(orderId, imageUrl);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Comprobante subido exitosamente",
                    "imageUrl", imageUrl,
                    "order", order
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al subir el comprobante: " + e.getMessage()));
        }
    }

}

