package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.AdminRequest;
import com.rds.app_restaurante.dto.SignUpResponse;
import com.rds.app_restaurante.model.Admin;
import com.rds.app_restaurante.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {
    
    private final AdminService adminService;
    
    @PostMapping("/admins")
    public ResponseEntity<?> createAdmin(@Valid @RequestBody AdminRequest adminRequest) {
        try {
            Admin admin = adminService.createAdmin(adminRequest);
            SignUpResponse response = SignUpResponse.builder()
                    .userId(admin.getId())
                    .email(admin.getEmail())
                    .name(admin.getName())
                    .lastName(admin.getLastName())
                    .message("Administrador creado exitosamente")
                    .build();
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear el administrador");
        }
    }
    
}

