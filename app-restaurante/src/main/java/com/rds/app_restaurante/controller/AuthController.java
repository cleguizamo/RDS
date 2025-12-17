package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.LoginRequest;
import com.rds.app_restaurante.dto.LoginResponse;
import com.rds.app_restaurante.dto.SignUpRequest;
import com.rds.app_restaurante.dto.SignUpResponse;
import com.rds.app_restaurante.dto.ForgotPasswordRequest;
import com.rds.app_restaurante.dto.ResetPasswordRequest;
import com.rds.app_restaurante.dto.VerifyResetCodeRequest;
import com.rds.app_restaurante.dto.CheckEmailRequest;
import com.rds.app_restaurante.exception.AuthenticationException;
import com.rds.app_restaurante.service.AuthService;
import com.rds.app_restaurante.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor

@Slf4j
public class AuthController {
    
    private final AuthService authService;
    private final UserService userService;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            log.debug("Login request received for email: {}", loginRequest.getEmail());
            LoginResponse response = authService.authenticate(loginRequest);
            log.info("Login successful for email: {} with role: {}", loginRequest.getEmail(), response.getRole());
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for email: {} - {}", loginRequest.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Credenciales inválidas"));
        } catch (Exception e) {
            log.error("Error processing login request for email: {}", loginRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al procesar la solicitud"));
        }
    }
    
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        try {
            log.info("Signup request received for email: {}", signUpRequest.getEmail());
            var user = userService.signUp(signUpRequest);
            log.info("User signup successful with ID: {} and email: {}", user.getId(), user.getEmail());
            SignUpResponse response = SignUpResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .lastName(user.getLastName())
                    .message("Usuario registrado exitosamente")
                    .build();
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.warn("Signup failed for email: {} - {}", signUpRequest.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            log.error("Error processing signup request for email: {}", signUpRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar el registro");
        }
    }

    @PostMapping("/check-email")
    public ResponseEntity<?> checkEmail(@Valid @RequestBody CheckEmailRequest request) {
        try {
            log.info("Check email request received: {}", request.getEmail());
            boolean exists = userService.emailExists(request.getEmail());
            if (exists) {
                // Si el email existe, generar y enviar el código
                userService.generatePasswordResetCode(request.getEmail());
                return ResponseEntity.ok(Map.of("exists", true, "message", "Código enviado a tu correo electrónico"));
            } else {
                return ResponseEntity.ok(Map.of("exists", false, "message", "El email no está registrado"));
            }
        } catch (Exception e) {
            log.error("Error checking email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("exists", false, "message", "Error al procesar la solicitud"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            log.info("Forgot password request received for email: {}", request.getEmail());
            userService.generatePasswordResetCode(request.getEmail());
            // No revelar si el email existe o no por seguridad
            return ResponseEntity.ok(Map.of("message", "Si el email existe, recibirás un código de 8 dígitos para restablecer tu contraseña"));
        } catch (RuntimeException e) {
            // No revelar si el email existe o no por seguridad
            log.warn("Forgot password failed for email: {} - {}", request.getEmail(), e.getMessage());
            return ResponseEntity.ok(Map.of("message", "Si el email existe, recibirás un código de 8 dígitos para restablecer tu contraseña"));
        } catch (Exception e) {
            log.error("Error processing forgot password request for email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al procesar la solicitud"));
        }
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        try {
            log.info("Verify reset code request received for email: {}", request.getEmail());
            boolean isValid = userService.verifyResetCode(request.getEmail(), request.getCode());
            if (isValid) {
                return ResponseEntity.ok(Map.of("valid", true, "message", "Código válido"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("valid", false, "message", "Código inválido o expirado"));
            }
        } catch (RuntimeException e) {
            log.warn("Verify reset code failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("valid", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error verifying reset code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("valid", false, "message", "Error al verificar el código"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            log.info("Reset password request received for email: {}", request.getEmail());
            // Verificar código antes de resetear
            boolean isValid = userService.verifyResetCode(request.getEmail(), request.getCode());
            if (!isValid) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Código inválido o expirado"));
            }
            userService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Contraseña restablecida exitosamente"));
        } catch (RuntimeException e) {
            log.warn("Reset password failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing reset password request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al procesar la solicitud"));
        }
    }
}

