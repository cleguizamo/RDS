package com.rds.app_restaurante.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rds.app_restaurante.model.User;
import com.rds.app_restaurante.repository.UserRepository;
import com.rds.app_restaurante.dto.UserRequest;
import com.rds.app_restaurante.dto.UserUpdateRequest;
import com.rds.app_restaurante.dto.UserResponse;
import com.rds.app_restaurante.dto.SignUpRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

//Servicio para manejar las operaciones de usuario
@Service
@RequiredArgsConstructor
public class UserService {
    //Repositorio de usuarios
    private final UserRepository userRepository;
    //Encriptador de contraseñas
    private final PasswordEncoder passwordEncoder;

    //Metodo para hashear la contraseña y crear un nuevo usuario (para uso interno)
    public User create(UserRequest userRequest) {
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
        return userRepository.save(user);
    }

    //Metodo para registro público de usuarios (solo CLIENT)
    public User signUp(SignUpRequest signUpRequest) {
        // Verificar si el email ya existe
        if (userRepository.findByEmail(signUpRequest.getEmail()).isPresent()) {
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
        
        return userRepository.save(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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