package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.LoginRequest;
import com.rds.app_restaurante.dto.LoginResponse;
import com.rds.app_restaurante.model.Admin;
import com.rds.app_restaurante.model.Employee;
import com.rds.app_restaurante.model.Role;
import com.rds.app_restaurante.model.User;
import com.rds.app_restaurante.repository.AdminRepository;
import com.rds.app_restaurante.repository.EmployeeRepository;
import com.rds.app_restaurante.repository.UserRepository;
import com.rds.app_restaurante.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    public LoginResponse authenticate(LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();
        
        System.out.println("=== Intentando autenticar ===");
        System.out.println("Email recibido: " + email);
        System.out.println("Password recibido: " + (password != null ? "***" : "null"));
        
        // 1. Buscar en la tabla de usuarios (CLIENT)
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            System.out.println("Usuario encontrado en tabla USER");
            boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
            System.out.println("Password matches: " + passwordMatches);
            if (passwordMatches) {
                System.out.println("Autenticación exitosa como CLIENT");
                String token = jwtUtil.generateToken(user.getId(), user.getEmail(), Role.CLIENT);
                return LoginResponse.builder()
                        .token(token)
                        .email(user.getEmail())
                        .name(user.getName())
                        .lastName(user.getLastName())
                        .role(Role.CLIENT)
                        .userId(user.getId())
                        .redirectTo("/dashboard")
                        .build();
            } else {
                System.out.println("Password no coincide para USER");
            }
        } else {
            System.out.println("Usuario no encontrado en tabla USER");
        }
        
        // 2. Buscar en la tabla de administradores (ADMIN)
        Optional<Admin> adminOpt = adminRepository.findByEmail(email);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            System.out.println("Usuario encontrado en tabla ADMIN");
            boolean passwordMatches = passwordEncoder.matches(password, admin.getPassword());
            System.out.println("Password matches: " + passwordMatches);
            System.out.println("Password hash almacenado: " + admin.getPassword());
            if (passwordMatches) {
                System.out.println("Autenticación exitosa como ADMIN");
                String token = jwtUtil.generateToken(admin.getId(), admin.getEmail(), Role.ADMIN);
                return LoginResponse.builder()
                        .token(token)
                        .email(admin.getEmail())
                        .name(admin.getName())
                        .lastName(admin.getLastName())
                        .role(Role.ADMIN)
                        .userId(admin.getId())
                        .redirectTo("/admin")
                        .build();
            } else {
                System.out.println("Password no coincide para ADMIN");
            }
        } else {
            System.out.println("Usuario no encontrado en tabla ADMIN");
        }
        
        // 3. Buscar en la tabla de empleados (EMPLOYEE)
        Optional<Employee> employeeOpt = employeeRepository.findByEmail(email);
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            System.out.println("Usuario encontrado en tabla EMPLOYEE");
            boolean passwordMatches = passwordEncoder.matches(password, employee.getPassword());
            System.out.println("Password matches: " + passwordMatches);
            if (passwordMatches) {
                System.out.println("Autenticación exitosa como EMPLOYEE");
                String token = jwtUtil.generateToken(employee.getId(), employee.getEmail(), Role.EMPLOYEE);
                return LoginResponse.builder()
                        .token(token)
                        .email(employee.getEmail())
                        .name(employee.getName())
                        .lastName(employee.getLastName())
                        .role(Role.EMPLOYEE)
                        .userId(employee.getId())
                        .redirectTo("/employee")
                        .build();
            } else {
                System.out.println("Password no coincide para EMPLOYEE");
            }
        } else {
            System.out.println("Usuario no encontrado en tabla EMPLOYEE");
        }
        
        System.out.println("=== Autenticación fallida: Credenciales inválidas ===");
        throw new com.rds.app_restaurante.exception.AuthenticationException("Credenciales inválidas");
    }
}

