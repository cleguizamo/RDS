package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    Optional<User> findByResetPasswordCode(String code);
    
    // Búsqueda por nombre
    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    // Búsqueda por email
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);
    
    // Búsqueda combinada: nombre o apellido
    @Query("SELECT u FROM User u WHERE " +
           "(:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:documentNumber IS NULL OR u.documentNumber LIKE CONCAT('%', :documentNumber, '%')) AND " +
           "(:phone IS NULL OR CAST(u.phone AS string) LIKE CONCAT('%', :phone, '%')) AND " +
           "(:minPoints IS NULL OR u.points >= :minPoints) AND " +
           "(:maxPoints IS NULL OR u.points <= :maxPoints)")
    Page<User> searchUsers(
            @Param("name") String name,
            @Param("email") String email,
            @Param("documentNumber") String documentNumber,
            @Param("phone") String phone,
            @Param("minPoints") Long minPoints,
            @Param("maxPoints") Long maxPoints,
            Pageable pageable
    );
    
    // Búsqueda por rango de puntos
    Page<User> findByPointsBetween(Long minPoints, Long maxPoints, Pageable pageable);
    
    // Búsqueda por fecha de registro (últimos N días)
    @Query("SELECT u FROM User u WHERE u.dateOfBirth >= :sinceDate")
    Page<User> findByDateOfBirthAfter(@Param("sinceDate") LocalDate sinceDate, Pageable pageable);
}
