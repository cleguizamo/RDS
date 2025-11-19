package com.rds.app_restaurante.config;

import com.rds.app_restaurante.model.Admin;
import com.rds.app_restaurante.model.DocumentType;
import com.rds.app_restaurante.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

//Configuracion para inicializar datos predeterminados
@Configuration
@RequiredArgsConstructor
public class DataInitializer {
    
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Bean
    public CommandLineRunner initDefaultAdmin() {
        return args -> {
            // Solo crear admin si no existe ninguno
            if (adminRepository.count() == 0) {
                Admin defaultAdmin = new Admin(
                    "Admin",
                    "Principal",
                    DocumentType.CC,
                    "0000000000",
                    "admin@restaurante.com",
                    passwordEncoder.encode("admin123"), // Cambiar en producción
                    Long.parseLong("3000000000")
                );
                adminRepository.save(defaultAdmin);
                System.out.println("========================================");
                System.out.println("Admin predeterminado creado:");
                System.out.println("Email: admin@restaurante.com");
                System.out.println("Password: admin123");
                System.out.println("¡IMPORTANTE: Cambiar la contraseña después del primer login!");
                System.out.println("========================================");
            }
        };
    }
}

