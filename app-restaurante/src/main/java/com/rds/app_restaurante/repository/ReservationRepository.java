package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserId(Long userId);
    List<Reservation> findByDate(LocalDate date);
    List<Reservation> findByStatus(boolean status);
    List<Reservation> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.date BETWEEN :startDate AND :endDate")
    Long countReservationsBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

