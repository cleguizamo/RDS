package com.rds.app_restaurante.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyResetCodeRequest {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    @NotBlank(message = "El código es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El código debe tener exactamente 8 dígitos")
    private String code;
}

