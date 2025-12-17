package com.rds.app_restaurante.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rds.app_restaurante.model.User;
import com.rds.app_restaurante.repository.UserRepository;
import com.rds.app_restaurante.dto.UserRequest;
import com.rds.app_restaurante.dto.UserUpdateRequest;
import com.rds.app_restaurante.dto.UserResponse;
import com.rds.app_restaurante.dto.UserSearchRequest;
import com.rds.app_restaurante.dto.SignUpRequest;
import com.rds.app_restaurante.service.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

//Servicio para manejar las operaciones de usuario
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    //Repositorio de usuarios
    private final UserRepository userRepository;
    //Encriptador de contraseñas
    private final PasswordEncoder passwordEncoder;
    //Servicio de email
    private final EmailService emailService;

    //Metodo para hashear la contraseña y crear un nuevo usuario (para uso interno)
    public User create(UserRequest userRequest) {
        log.info("Creating new user: {}", userRequest.getEmail());
        User user = new User(
            userRequest.getName(),
            userRequest.getLastName(),
            userRequest.getDocumentType(),
            userRequest.getDocumentNumber(),
            userRequest.getPhone(),
            passwordEncoder.encode(userRequest.getPassword()),
            userRequest.getEmail(),
            userRequest.getDateOfBirth()
        );
        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());
        return savedUser;
    }

    //Metodo para registro público de usuarios (solo CLIENT)
    public User signUp(SignUpRequest signUpRequest) {
        log.info("User signup attempt for email: {}", signUpRequest.getEmail());
        // Verificar si el email ya existe
        if (userRepository.findByEmail(signUpRequest.getEmail()).isPresent()) {
            log.warn("Signup attempt with existing email: {}", signUpRequest.getEmail());
            throw new RuntimeException("El email ya está registrado");
        }
        
        User user = new User(
            signUpRequest.getName(),
            signUpRequest.getLastName(),
            signUpRequest.getDocumentType(),
            signUpRequest.getDocumentNumber(),
            Long.parseLong(signUpRequest.getPhone()),
            passwordEncoder.encode(signUpRequest.getPassword()),
            signUpRequest.getEmail(),
            signUpRequest.getDateOfBirth()
        );
        
        User savedUser = userRepository.save(user);
        log.info("User signed up successfully with ID: {}", savedUser.getId());
        return savedUser;
    }

    public List<UserResponse> getAllUsers() {
        log.debug("Fetching all users from database");
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Page<UserResponse> getAllUsersPaginated(Pageable pageable) {
        log.debug("Fetching users page: {} with size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    public Page<UserResponse> searchUsers(UserSearchRequest searchRequest) {
        log.debug("Searching users with filters: {}", searchRequest);
        
        // Configurar paginación y ordenamiento
        int page = searchRequest.getPage() != null ? searchRequest.getPage() : 0;
        int size = searchRequest.getSize() != null ? searchRequest.getSize() : 20;
        
        Sort sort = Sort.unsorted();
        if (searchRequest.getSortBy() != null && !searchRequest.getSortBy().isEmpty()) {
            Sort.Direction direction = Sort.Direction.ASC;
            if (searchRequest.getSortDirection() != null && 
                searchRequest.getSortDirection().equalsIgnoreCase("DESC")) {
                direction = Sort.Direction.DESC;
            }
            sort = Sort.by(direction, searchRequest.getSortBy());
        }
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Ejecutar búsqueda
        Page<User> users = userRepository.searchUsers(
                searchRequest.getName(),
                searchRequest.getEmail(),
                searchRequest.getDocumentNumber(),
                searchRequest.getPhone(),
                searchRequest.getMinPoints(),
                searchRequest.getMaxPoints(),
                pageable
        );
        
        log.debug("Found {} users matching search criteria", users.getTotalElements());
        return users.map(this::mapToResponse);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest userUpdateRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));

        // Verificar si el email ya está en uso por otro usuario
        userRepository.findByEmail(userUpdateRequest.getEmail())
                .ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(id)) {
                        throw new RuntimeException("El email ya está en uso por otro usuario");
                    }
                });

        user.setName(userUpdateRequest.getName());
        user.setLastName(userUpdateRequest.getLastName());
        user.setDocumentType(userUpdateRequest.getDocumentType());
        user.setDocumentNumber(userUpdateRequest.getDocumentNumber());
        user.setPhone(userUpdateRequest.getPhone());
        user.setEmail(userUpdateRequest.getEmail());
        user.setDateOfBirth(userUpdateRequest.getDateOfBirth());
        
        if (userUpdateRequest.getPoints() != null) {
            user.setPoints(userUpdateRequest.getPoints());
        }
        
        if (userUpdateRequest.getTotalSpent() != null) {
            user.setTotalSpent(userUpdateRequest.getTotalSpent());
        }

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado con id: " + id);
        }
        userRepository.deleteById(id);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Transactional
    public void generatePasswordResetCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));
        
        // Generar código de 8 dígitos
        Random random = new Random();
        String code = String.format("%08d", random.nextInt(100000000));
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(15); // Código válido por 15 minutos
        
        user.setResetPasswordCode(code);
        user.setResetPasswordCodeExpiry(expiryDate);
        userRepository.save(user);
        
        // Enviar email con el código
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName() + " " + user.getLastName(), code);
            log.info("Password reset code generated and email sent for user: {}", email);
        } catch (Exception e) {
            log.error("Error sending password reset email to {}: {}", email, e.getMessage());
            // No fallar la operación si el email no se puede enviar
        }
    }

    public boolean verifyResetCode(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));
        
        // Verificar que el código existe y no ha expirado
        if (user.getResetPasswordCode() == null || !user.getResetPasswordCode().equals(code)) {
            return false;
        }
        
        if (user.getResetPasswordCodeExpiry() == null || 
            user.getResetPasswordCodeExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        return true;
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));
        
        // Verificar código
        if (user.getResetPasswordCode() == null || !user.getResetPasswordCode().equals(code)) {
            throw new RuntimeException("Código inválido");
        }
        
        // Verificar que el código no haya expirado
        if (user.getResetPasswordCodeExpiry() == null || 
            user.getResetPasswordCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Código expirado. Por favor, solicita un nuevo código.");
        }
        
        // Actualizar contraseña y limpiar código
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordCode(null);
        user.setResetPasswordCodeExpiry(null);
        userRepository.save(user);
        
        log.info("Password reset successful for user: {}", user.getEmail());
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));
        
        // Verificar contraseña actual
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }
        
        // Actualizar contraseña
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("Password changed successfully for user: {}", user.getEmail());
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .lastName(user.getLastName())
                .documentType(user.getDocumentType())
                .documentNumber(user.getDocumentNumber())
                .phone(user.getPhone())
                .email(user.getEmail())
                .points(user.getPoints())
                .dateOfBirth(user.getDateOfBirth())
                .numberOfOrders(user.getNumberOfOrders())
                .totalSpent(user.getTotalSpent())
                .lastOrderDate(user.getLastOrderDate())
                .numberOfReservations(user.getNumberOfReservations())
                .build();
    }
}