package com.rds.app_restaurante.controller;

import com.rds.app_restaurante.dto.EmployeeRequest;
import com.rds.app_restaurante.dto.EmployeeResponse;
import com.rds.app_restaurante.dto.SalaryPaymentResponse;
import com.rds.app_restaurante.dto.SalaryUpdateRequest;
import com.rds.app_restaurante.dto.SignUpResponse;
import com.rds.app_restaurante.model.Employee;
import com.rds.app_restaurante.service.EmployeeService;
import com.rds.app_restaurante.service.SalaryPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/employees")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmployeeController {

    private final EmployeeService employeeService;
    private final SalaryPaymentService salaryPaymentService;

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {
        List<EmployeeResponse> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEmployeeById(@PathVariable("id") Long id) {
        try {
            EmployeeResponse employee = employeeService.getEmployeeById(id);
            return ResponseEntity.ok(employee);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createEmployee(@Valid @RequestBody EmployeeRequest employeeRequest) {
        try {
            Employee employee = employeeService.createEmployee(employeeRequest);
            SignUpResponse response = SignUpResponse.builder()
                    .userId(employee.getId())
                    .email(employee.getEmail())
                    .name(employee.getName())
                    .lastName(employee.getLastName())
                    .message("Empleado creado exitosamente")
                    .build();
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al crear el empleado"));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Error de validación");
        response.put("errors", errors);
        response.put("validationFailed", true);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PutMapping("/{id}/salary")
    public ResponseEntity<?> updateEmployeeSalary(
            @PathVariable("id") Long id,
            @Valid @RequestBody SalaryUpdateRequest salaryUpdateRequest) {
        try {
            EmployeeResponse employee = employeeService.updateEmployeeSalary(id, salaryUpdateRequest);
            return ResponseEntity.ok(employee);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar el sueldo del empleado");
        }
    }

    @GetMapping("/{id}/salary-payments")
    public ResponseEntity<?> getEmployeeSalaryPayments(@PathVariable("id") Long id) {
        try {
            List<SalaryPaymentResponse> payments = salaryPaymentService.getPaymentsByEmployee(id);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener los pagos del empleado");
        }
    }

    @GetMapping("/salary-payments")
    public ResponseEntity<?> getAllSalaryPayments(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<SalaryPaymentResponse> payments = salaryPaymentService.getAllPayments(startDate, endDate);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener los pagos de sueldos");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable("id") Long id) {
        try {
            employeeService.deleteEmployee(id);
            return ResponseEntity.ok(Map.of("message", "Empleado eliminado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar el empleado");
        }
    }
}

