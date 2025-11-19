package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.DeliveryRequest;
import com.rds.app_restaurante.dto.DeliveryResponse;
import com.rds.app_restaurante.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/admin/deliveries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeliveryResponse>> getAllDeliveries() {
        List<DeliveryResponse> deliveries = deliveryService.getAllDeliveries();
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/admin/deliveries/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDeliveryById(@PathVariable Long id) {
        try {
            DeliveryResponse delivery = deliveryService.getDeliveryById(id);
            return ResponseEntity.ok(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/admin/deliveries/date/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeliveryResponse>> getDeliveriesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DeliveryResponse> deliveries = deliveryService.getDeliveriesByDate(date);
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/employee/deliveries")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<DeliveryResponse>> getAllDeliveriesForEmployee() {
        List<DeliveryResponse> deliveries = deliveryService.getAllDeliveries();
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/employee/deliveries/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> getDeliveryByIdForEmployee(@PathVariable Long id) {
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
    public ResponseEntity<?> updateDeliveryStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
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
            @RequestParam(required = false) Long userId) {
        if (userId != null) {
            List<DeliveryResponse> deliveries = deliveryService.getDeliveriesByUserId(userId);
            return ResponseEntity.ok(deliveries);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}

