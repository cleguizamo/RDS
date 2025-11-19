package com.rds.app_restaurante.dto;

import com.rds.app_restaurante.model.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String lastName;
    private DocumentType documentType;
    private String documentNumber;
    private Long phone;
    private String email;
    private long points;
    private LocalDate dateOfBirth;
    private long numberOfOrders;
    private double totalSpent;
    private LocalDate lastOrderDate;
    private long numberOfReservations;
}

