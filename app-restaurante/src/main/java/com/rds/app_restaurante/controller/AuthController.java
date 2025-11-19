package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.LoginRequest;
import com.rds.app_restaurante.dto.LoginResponse;
import com.rds.app_restaurante.dto.SignUpRequest;
import com.rds.app_restaurante.dto.SignUpResponse;
import com.rds.app_restaurante.exception.AuthenticationException;
import com.rds.app_restaurante.service.AuthService;
import com.rds.app_restaurante.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {
    
    private final AuthService authService;
    private final UserService userService;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            System.out.println("Login request recibido para: " + loginRequest.getEmail());
            LoginResponse response = authService.authenticate(loginRequest);
            System.out.println("Login exitoso para: " + loginRequest.getEmail());
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            System.err.println("AuthenticationException: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Credenciales inválidas"));
        } catch (Exception e) {
            System.err.println("Exception en login: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al procesar la solicitud: " + e.getMessage()));
        }
    }
    
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        try {
            var user = userService.signUp(signUpRequest);
            SignUpResponse response = SignUpResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .lastName(user.getLastName())
                    .message("Usuario registrado exitosamente")
                    .build();
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar el registro");
        }
    }
}

