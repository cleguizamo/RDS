package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.ReservationRequest;
import com.rds.app_restaurante.dto.ReservationResponse;
import com.rds.app_restaurante.model.Reservation;
import com.rds.app_restaurante.model.User;
import com.rds.app_restaurante.repository.ReservationRepository;
import com.rds.app_restaurante.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con id: " + id));
        return mapToResponse(reservation);
    }

    public List<ReservationResponse> getReservationsByUserId(Long userId) {
        return reservationRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ReservationResponse> getReservationsByDate(LocalDate date) {
        return reservationRepository.findByDate(date).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ReservationResponse> getReservationsByStatus(boolean status) {
        return reservationRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReservationResponse createReservation(ReservationRequest reservationRequest, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));

        Reservation reservation = new Reservation(
                reservationRequest.getDate(),
                reservationRequest.getTime(),
                reservationRequest.getNumberOfPeople(),
                false, // Pendiente por defecto
                reservationRequest.getNotes(),
                user
        );

        Reservation savedReservation = reservationRepository.save(reservation);

        // Actualizar estadísticas del usuario
        user.setNumberOfReservations(user.getNumberOfReservations() + 1);
        userRepository.save(user);

        // Enviar email de reserva pendiente (no confirmada aún)
        try {
            emailService.sendReservationPendingEmail(
                    user.getEmail(),
                    user.getName() + " " + user.getLastName(),
                    savedReservation.getId(),
                    savedReservation.getDate(),
                    savedReservation.getTime(),
                    savedReservation.getNumberOfPeople(),
                    savedReservation.getNotes()
            );
        } catch (Exception e) {
            log.warn("Error enviando email de reserva pendiente: {}", e.getMessage());
            // No fallar la creación de la reserva si falla el email
        }

        return mapToResponse(savedReservation);
    }

    @Transactional
    public ReservationResponse confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con id: " + id));
        
        boolean wasConfirmed = reservation.isStatus();
        reservation.setStatus(true);
        Reservation confirmedReservation = reservationRepository.save(reservation);
        
        // Enviar email de confirmación si la reserva acaba de ser confirmada
        if (!wasConfirmed) {
            User user = reservation.getUser();
            try {
                emailService.sendReservationConfirmationEmail(
                        user.getEmail(),
                        user.getName() + " " + user.getLastName(),
                        confirmedReservation.getId(),
                        confirmedReservation.getDate(),
                        confirmedReservation.getTime(),
                        confirmedReservation.getNumberOfPeople(),
                        confirmedReservation.getNotes()
                );
            } catch (Exception e) {
                log.warn("Error enviando email de confirmación de reserva: {}", e.getMessage());
            }
        }
        
        return mapToResponse(confirmedReservation);
    }

    @Transactional
    public void deleteReservation(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new RuntimeException("Reserva no encontrada con id: " + id);
        }
        reservationRepository.deleteById(id);
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .date(reservation.getDate())
                .time(reservation.getTime())
                .numberOfPeople(reservation.getNumberOfPeople())
                .status(reservation.isStatus())
                .notes(reservation.getNotes())
                .userId(reservation.getUser().getId())
                .userName(reservation.getUser().getName() + " " + reservation.getUser().getLastName())
                .userEmail(reservation.getUser().getEmail())
                .build();
    }
}

