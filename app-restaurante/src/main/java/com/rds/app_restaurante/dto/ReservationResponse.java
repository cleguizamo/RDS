package com.rds.app_restaurante.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private Long id;
    private LocalDate date;
    private LocalTime time;
    private Integer numberOfPeople;
    private Boolean status;
    private String notes;
    private Long userId;
    private String userName;
    private String userEmail;
}

