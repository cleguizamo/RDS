package com.rds.app_restaurante.dto;

import com.rds.app_restaurante.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//DTO para la respuesta de login
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String email;
    private String name;
    private String lastName;
    private Role role;
    private Long userId;
    private String redirectTo;
}

