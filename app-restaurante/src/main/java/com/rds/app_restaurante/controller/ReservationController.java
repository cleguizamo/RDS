package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.ReservationRequest;
import com.rds.app_restaurante.dto.ReservationResponse;
import com.rds.app_restaurante.service.ReservationService;
import com.rds.app_restaurante.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
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
public class ReservationController {

    private final ReservationService reservationService;
    private final JwtUtil jwtUtil;

    @GetMapping("/admin/reservations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservationResponse>> getAllReservations() {
        List<ReservationResponse> reservations = reservationService.getAllReservations();
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/admin/reservations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getReservationById(@PathVariable Long id) {
        try {
            ReservationResponse reservation = reservationService.getReservationById(id);
            return ResponseEntity.ok(reservation);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/admin/reservations/date/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservationResponse>> getReservationsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ReservationResponse> reservations = reservationService.getReservationsByDate(date);
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/admin/reservations/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservationResponse>> getReservationsByStatus(@PathVariable boolean status) {
        List<ReservationResponse> reservations = reservationService.getReservationsByStatus(status);
        return ResponseEntity.ok(reservations);
    }

    @PutMapping("/employee/reservations/{id}/confirm")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> confirmReservation(@PathVariable Long id) {
        try {
            ReservationResponse reservation = reservationService.confirmReservation(id);
            return ResponseEntity.ok(reservation);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al confirmar la reserva");
        }
    }

    @GetMapping("/employee/reservations")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<ReservationResponse>> getAllReservationsForEmployee() {
        List<ReservationResponse> reservations = reservationService.getAllReservations();
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/employee/reservations/status/{status}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<ReservationResponse>> getReservationsByStatusForEmployee(@PathVariable boolean status) {
        List<ReservationResponse> reservations = reservationService.getReservationsByStatus(status);
        return ResponseEntity.ok(reservations);
    }

    @DeleteMapping("/admin/reservations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteReservation(@PathVariable Long id) {
        try {
            reservationService.deleteReservation(id);
            return ResponseEntity.ok(Map.of("message", "Reserva eliminada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar la reserva");
        }
    }

    // Endpoints para clientes
    @PostMapping("/client/reservations")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> createReservation(@Valid @RequestBody ReservationRequest reservationRequest, HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token no válido");
            }

            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);

            ReservationResponse reservation = reservationService.createReservation(reservationRequest, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear la reserva");
        }
    }

    @GetMapping("/client/reservations")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getClientReservations(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token no válido");
            }

            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);

            List<ReservationResponse> reservations = reservationService.getReservationsByUserId(userId);
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener las reservas");
        }
    }
}

