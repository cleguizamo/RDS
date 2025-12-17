package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.UserResponse;
import com.rds.app_restaurante.dto.UserUpdateRequest;
import com.rds.app_restaurante.dto.UserSearchRequest;
import com.rds.app_restaurante.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor

public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            // Parámetros de búsqueda avanzada
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String documentNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Long minPoints,
            @RequestParam(required = false) Long maxPoints,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        
        // Si hay algún filtro de búsqueda, usar búsqueda avanzada
        if (name != null || email != null || documentNumber != null || 
            phone != null || minPoints != null || maxPoints != null ||
            sortBy != null) {
            
            UserSearchRequest searchRequest = UserSearchRequest.builder()
                    .name(name)
                    .email(email)
                    .documentNumber(documentNumber)
                    .phone(phone)
                    .minPoints(minPoints)
                    .maxPoints(maxPoints)
                    .page(page)
                    .size(size)
                    .sortBy(sortBy)
                    .sortDirection(sortDirection)
                    .build();
            
            Page<UserResponse> usersPage = userService.searchUsers(searchRequest);
            return ResponseEntity.ok(usersPage);
        }
        
        // Sin filtros, comportamiento normal
        if (page == 0 && size == 20) {
            List<UserResponse> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } else {
            Pageable pageable = PageRequest.of(page, size);
            Page<UserResponse> usersPage = userService.getAllUsersPaginated(pageable);
            return ResponseEntity.ok(usersPage);
        }
    }
    
    @PostMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestBody UserSearchRequest searchRequest) {
        try {
            Page<UserResponse> usersPage = userService.searchUsers(searchRequest);
            return ResponseEntity.ok(usersPage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error en la búsqueda: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            UserResponse user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
        try {
            UserResponse user = userService.updateUser(id, userUpdateRequest);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar el usuario");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "Usuario eliminado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar el usuario");
        }
    }
}

