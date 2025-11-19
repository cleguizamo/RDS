package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserId(Long userId);
    List<Reservation> findByDate(LocalDate date);
    List<Reservation> findByStatus(boolean status);
}

