package com.rds.app_restaurante.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import java.time.LocalDate;

//Clase para representar el usuario en la base de datos
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    //Id del usuario, generado automaticamente por la base de datos
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Nombre del usuario
    @Column(name = "name", nullable = false)
    private String name;

    //Apellido del usuario
    @Column(name = "last_name", nullable = false)
    private String lastName;

    //Tipo de documento del usuario
    @Column(name = "document_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    //Numero de documento del usuario
    @Column(name = "document_number", nullable = false, unique = true)
    private String documentNumber;

    //Telefono del usuario
    @Column(name = "phone", nullable = false, unique = true)
    private Long phone;

    //Contraseña del usuario (Se guarda hasheada)
    @Column(name = "password", nullable = false)
    private String password;

    //Email del usuario
    @Column(name = "email", nullable = false)
    private String email;

    //Puntos del usuario
    @Column(name = "points", nullable = false)
    private long points;

    //Fecha de nacimiento del usuario
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    //Numero de pedidos del usuario
    @Column(name = "number_of_orders", nullable = false)
    private long numberOfOrders;

    //Total gastado por el usuario
    @Column(name = "total_spent", nullable = false)
    private double totalSpent;

    //Fecha del ultimo pedido del usuario
    @Column(name = "last_order_date", nullable = true)
    private LocalDate lastOrderDate;

    //Numero de reservaciones del usuario
    @Column(name = "number_of_reservations", nullable = false)
    private long numberOfReservations;

    //Código de 8 dígitos para recuperación de contraseña
    @Column(name = "reset_password_code", nullable = true, length = 8)
    private String resetPasswordCode;

    //Fecha de expiración del código de recuperación
    @Column(name = "reset_password_code_expiry", nullable = true)
    private java.time.LocalDateTime resetPasswordCodeExpiry;

    //Constructor para crear un nuevo usuario (Sin ID ya que lo genera la base de datos)
    public User(String name, String lastName, DocumentType documentType, String documentNumber, Long phone, String password, String email, LocalDate dateOfBirth) {
        this.name = name;
        this.lastName = lastName;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.phone = phone;
        this.password = password;
        this.email = email;
        this.points = 0; //Puntos iniciales del usuario
        this.dateOfBirth = dateOfBirth;
        this.numberOfOrders = 0; //Numero de pedidos iniciales del usuario
        this.totalSpent = 0; //Total gastado inicial del usuario
        this.lastOrderDate = null; //Fecha del ultimo pedido inicial del usuario
        this.numberOfReservations = 0; //Numero de reservaciones iniciales del usuario
    }
    
    
}
