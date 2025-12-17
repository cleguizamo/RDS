package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.EmployeeRequest;
import com.rds.app_restaurante.dto.EmployeeResponse;
import com.rds.app_restaurante.dto.SalaryUpdateRequest;
import com.rds.app_restaurante.model.Employee;
import com.rds.app_restaurante.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public Employee createEmployee(EmployeeRequest employeeRequest) {
        if (employeeRepository.findByEmail(employeeRequest.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado como empleado");
        }

        Employee employee = new Employee(
                employeeRequest.getName(),
                employeeRequest.getLastName(),
                employeeRequest.getDocumentType(),
                employeeRequest.getDocumentNumber(),
                employeeRequest.getEmail(),
                passwordEncoder.encode(employeeRequest.getPassword()),
                Long.parseLong(employeeRequest.getPhone())
        );
        return employeeRepository.save(employee);
    }

    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con id: " + id));
        return mapToResponse(employee);
    }

    @Transactional
    public EmployeeResponse updateEmployeeSalary(Long id, SalaryUpdateRequest salaryUpdateRequest) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con id: " + id));
        
        employee.setSalary(salaryUpdateRequest.getSalary());
        employee.setPaymentFrequency(salaryUpdateRequest.getPaymentFrequency());
        employee.setPaymentDay(salaryUpdateRequest.getPaymentDay());
        
        Employee savedEmployee = employeeRepository.save(employee);
        return mapToResponse(savedEmployee);
    }

    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Empleado no encontrado con id: " + id);
        }
        employeeRepository.deleteById(id);
    }

    private EmployeeResponse mapToResponse(Employee employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .name(employee.getName())
                .lastName(employee.getLastName())
                .documentType(employee.getDocumentType())
                .documentNumber(employee.getDocumentNumber())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .salary(employee.getSalary())
                .paymentFrequency(employee.getPaymentFrequency())
                .paymentDay(employee.getPaymentDay())
                .build();
    }
}

