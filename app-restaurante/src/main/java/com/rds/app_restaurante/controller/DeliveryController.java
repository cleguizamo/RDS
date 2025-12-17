package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.DeliveryRequest;
import com.rds.app_restaurante.dto.DeliveryResponse;
import com.rds.app_restaurante.model.PaymentStatus;
import com.rds.app_restaurante.service.CloudinaryService;
import com.rds.app_restaurante.service.DeliveryService;
import com.rds.app_restaurante.util.JwtUtil;
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

public class DeliveryController {

    private final DeliveryService deliveryService;
    private final JwtUtil jwtUtil;
    private final CloudinaryService cloudinaryService;

    @GetMapping("/admin/deliveries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeliveryResponse>> getAllDeliveries() {
        List<DeliveryResponse> deliveries = deliveryService.getAllDeliveries();
        return ResponseEntity.ok(deliveries);
    }

    // IMPORTANTE: Rutas específicas SIN path variables DEBEN ir ANTES de rutas con {id}
    // Esto evita que Spring intente convertir "verified-payments" a Long
    @GetMapping("/admin/deliveries/pending-payments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeliveryResponse>> getDeliveriesWithPendingPayments() {
        List<DeliveryResponse> deliveries = deliveryService.getDeliveriesByPaymentStatus(PaymentStatus.PENDING);
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/admin/deliveries/verified-payments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeliveryResponse>> getDeliveriesWithVerifiedPayments() {
        List<DeliveryResponse> deliveries = deliveryService.getDeliveriesByPaymentStatus(PaymentStatus.VERIFIED);
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/admin/deliveries/date/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeliveryResponse>> getDeliveriesByDate(
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DeliveryResponse> deliveries = deliveryService.getDeliveriesByDate(date);
        return ResponseEntity.ok(deliveries);
    }

    // Endpoint para administradores confirmar deliveries
    @PutMapping("/admin/deliveries/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateDeliveryStatusByAdmin(@PathVariable("id") Long id, @RequestBody Map<String, Boolean> request) {
        System.out.println("=== Endpoint updateDeliveryStatusByAdmin llamado con ID: " + id + " ===");
        try {
            Boolean status = request.get("status");
            if (status == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El campo 'status' es obligatorio");
            }
            DeliveryResponse delivery = deliveryService.updateDeliveryStatus(id, status);
            System.out.println("=== Delivery actualizado exitosamente: " + delivery.getId() + " ===");
            return ResponseEntity.ok(delivery);
        } catch (RuntimeException e) {
            System.err.println("=== Error al actualizar delivery: " + e.getMessage() + " ===");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("=== Error inesperado al actualizar delivery: " + e.getMessage() + " ===");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar el estado del domicilio");
        }
    }

    // IMPORTANTE: Esta ruta genérica DEBE ir al final, después de todas las rutas específicas
    @GetMapping("/admin/deliveries/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDeliveryById(@PathVariable("id") Long id) {
        try {
            DeliveryResponse delivery = deliveryService.getDeliveryById(id);
            return ResponseEntity.ok(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/employee/deliveries")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<DeliveryResponse>> getAllDeliveriesForEmployee() {
        List<DeliveryResponse> deliveries = deliveryService.getAllDeliveries();
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/employee/deliveries/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> getDeliveryByIdForEmployee(@PathVariable("id") Long id) {
        try {
            DeliveryResponse delivery = deliveryService.getDeliveryById(id);
            return ResponseEntity.ok(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/employee/deliveries")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> createDelivery(@Valid @RequestBody DeliveryRequest deliveryRequest) {
        try {
            DeliveryResponse delivery = deliveryService.createDelivery(deliveryRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear el domicilio");
        }
    }

    @PutMapping("/employee/deliveries/{id}/status")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> updateDeliveryStatus(@PathVariable("id") Long id, @RequestBody Map<String, Boolean> request) {
        try {
            Boolean status = request.get("status");
            if (status == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El campo 'status' es obligatorio");
            }
            DeliveryResponse delivery = deliveryService.updateDeliveryStatus(id, status);
            return ResponseEntity.ok(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar el estado del domicilio");
        }
    }

    @PostMapping("/client/deliveries")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> createClientDelivery(@Valid @RequestBody DeliveryRequest deliveryRequest) {
        try {
            DeliveryResponse delivery = deliveryService.createDelivery(deliveryRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear el domicilio");
        }
    }

    @GetMapping("/client/deliveries")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<DeliveryResponse>> getClientDeliveries(
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
            
            List<DeliveryResponse> deliveries = deliveryService.getDeliveriesByUserId(clientUserId);
            return ResponseEntity.ok(deliveries);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    // Endpoints para pagos (verificar y rechazar)
    @PostMapping("/admin/deliveries/{id}/verify-payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> verifyDeliveryPayment(
            @PathVariable("id") Long deliveryId,
            HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token de autenticación requerido"));
            }
            
            String token = authHeader.substring(7);
            Long adminId = jwtUtil.extractUserId(token);
            
            DeliveryResponse delivery = deliveryService.verifyPayment(deliveryId, adminId);
            return ResponseEntity.ok(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al verificar el pago"));
        }
    }

    @PostMapping("/admin/deliveries/{id}/reject-payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectDeliveryPayment(
            @PathVariable("id") Long deliveryId,
            HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token de autenticación requerido"));
            }
            
            String token = authHeader.substring(7);
            Long adminId = jwtUtil.extractUserId(token);
            
            DeliveryResponse delivery = deliveryService.rejectPayment(deliveryId, adminId);
            return ResponseEntity.ok(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al rechazar el pago"));
        }
    }

    // Endpoint para clientes subir comprobante de pago
    @PostMapping("/client/deliveries/{id}/upload-payment-proof")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> uploadDeliveryPaymentProof(
            @PathVariable("id") Long deliveryId,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "El archivo no puede estar vacío."));
            }
            String paymentProofUrl = cloudinaryService.upload(file).get("url").toString();
            DeliveryResponse delivery = deliveryService.updatePaymentProofUrl(deliveryId, paymentProofUrl);
            return ResponseEntity.ok(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al subir el comprobante de pago."));
        }
    }
}

