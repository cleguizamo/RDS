package com.rds.app_restaurante.service;

import com.rds.app_restaurante.Security.JwtUtil;
import com.rds.app_restaurante.dto.LoginRequest;
import com.rds.app_restaurante.dto.LoginResponse;
import com.rds.app_restaurante.model.Admin;
import com.rds.app_restaurante.model.Employee;
import com.rds.app_restaurante.model.Role;
import com.rds.app_restaurante.model.User;
import com.rds.app_restaurante.repository.AdminRepository;
import com.rds.app_restaurante.repository.EmployeeRepository;
import com.rds.app_restaurante.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    public LoginResponse authenticate(LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();
        
        log.debug("Attempting authentication for email: {}", email);
        
        // 1. Buscar en la tabla de usuarios (CLIENT)
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
            if (passwordMatches) {
                log.info("Successful authentication as CLIENT for email: {}", email);
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
                log.warn("Failed authentication attempt for CLIENT email: {} - Invalid password", email);
            }
        }
        
        // 2. Buscar en la tabla de administradores (ADMIN)
        Optional<Admin> adminOpt = adminRepository.findByEmail(email);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            boolean passwordMatches = passwordEncoder.matches(password, admin.getPassword());
            if (passwordMatches) {
                log.info("Successful authentication as ADMIN for email: {}", email);
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
                log.warn("Failed authentication attempt for ADMIN email: {} - Invalid password", email);
            }
        }
        
        // 3. Buscar en la tabla de empleados (EMPLOYEE)
        Optional<Employee> employeeOpt = employeeRepository.findByEmail(email);
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            boolean passwordMatches = passwordEncoder.matches(password, employee.getPassword());
            if (passwordMatches) {
                log.info("Successful authentication as EMPLOYEE for email: {}", email);
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
                log.warn("Failed authentication attempt for EMPLOYEE email: {} - Invalid password", email);
            }
        }
        
        log.warn("Authentication failed for email: {} - Invalid credentials", email);
        throw new com.rds.app_restaurante.exception.AuthenticationException("Credenciales inválidas");
    }
}

