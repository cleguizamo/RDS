package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.AdminRequest;
import com.rds.app_restaurante.model.Admin;
import com.rds.app_restaurante.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    
    public Admin createAdmin(AdminRequest adminRequest) {
        // Verificar si el email ya existe
        if (adminRepository.findByEmail(adminRequest.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }
        
        Admin admin = new Admin(
            adminRequest.getName(),
            adminRequest.getLastName(),
            adminRequest.getDocumentType(),
            adminRequest.getDocumentNumber(),
            adminRequest.getEmail(),
            passwordEncoder.encode(adminRequest.getPassword()),
            Long.parseLong(adminRequest.getPhone())
        );
        
        return adminRepository.save(admin);
    }
}

