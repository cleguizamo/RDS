package com.rds.app_restaurante.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "document_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Column(name = "document_number", nullable = false, unique = true)
    private String documentNumber;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "phone", nullable = false, unique = true)
    private Long phone;

    // Sueldo del empleado
    @Column(name = "salary", nullable = true, precision = 10, scale = 2)
    private BigDecimal salary;

    // Frecuencia de pago (mensual o quincenal)
    @Column(name = "payment_frequency", nullable = true)
    @Enumerated(EnumType.STRING)
    private PaymentFrequency paymentFrequency;

    // Día del mes en que se realiza el pago (1-31)
    @Column(name = "payment_day", nullable = true)
    private Integer paymentDay;

    // Constructor para crear un nuevo empleado (Sin ID ya que lo genera la base de datos)
    public Employee(String name, String lastName, DocumentType documentType, String documentNumber, String email, String password, Long phone) {
        this.name = name;
        this.lastName = lastName;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.salary = null;
        this.paymentFrequency = null;
        this.paymentDay = null;
    }
}

