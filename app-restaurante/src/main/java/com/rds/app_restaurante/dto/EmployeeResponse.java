package com.rds.app_restaurante.dto;

import com.rds.app_restaurante.model.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private Long id;
    private String name;
    private String lastName;
    private DocumentType documentType;
    private String documentNumber;
    private String email;
    private Long phone;
}

