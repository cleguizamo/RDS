package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.SalaryPaymentResponse;
import com.rds.app_restaurante.model.Employee;
import com.rds.app_restaurante.model.PaymentFrequency;
import com.rds.app_restaurante.model.PaymentStatus;
import com.rds.app_restaurante.model.SalaryPayment;
import com.rds.app_restaurante.repository.EmployeeRepository;
import com.rds.app_restaurante.repository.SalaryPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryPaymentService {

    private final SalaryPaymentRepository salaryPaymentRepository;
    private final EmployeeRepository employeeRepository;
    private final ExpenseService expenseService;
    private final BalanceService balanceService;
    private final AlertService alertService;
    private final EmailService emailService;

    @Transactional
    public void processSalaryPayments() {
        log.info("Iniciando procesamiento de pagos de sueldos...");
        LocalDate today = LocalDate.now();
        
        List<Employee> employees = employeeRepository.findAll()
                .stream()
                .filter(emp -> emp.getSalary() != null 
                        && emp.getPaymentFrequency() != null 
                        && emp.getPaymentDay() != null)
                .collect(Collectors.toList());
        
        log.info("Empleados con sueldo configurado: {}", employees.size());
        
        for (Employee employee : employees) {
            try {
                processEmployeePayment(employee, today);
            } catch (Exception e) {
                log.error("Error procesando pago para empleado {}: {}", employee.getId(), e.getMessage(), e);
            }
        }
        
        log.info("Procesamiento de pagos de sueldos completado");
    }

    private void processEmployeePayment(Employee employee, LocalDate today) {
        Integer paymentDay = employee.getPaymentDay();
        PaymentFrequency frequency = employee.getPaymentFrequency();
        BigDecimal salary = employee.getSalary();
        
        // Ajustar el día de pago si el mes actual no tiene ese día (ej: día 31 en febrero)
        int adjustedPaymentDay = adjustPaymentDay(paymentDay, today);
        
        // Verificar si hoy es el día de pago
        if (today.getDayOfMonth() != adjustedPaymentDay) {
            return;
        }
        
        // Calcular el período
        LocalDate periodStartDate;
        LocalDate periodEndDate;
        
        if (frequency == PaymentFrequency.MONTHLY) {
            // Pago mensual: período del mes anterior
            YearMonth previousMonth = YearMonth.from(today.minusMonths(1));
            periodStartDate = previousMonth.atDay(1);
            periodEndDate = previousMonth.atEndOfMonth();
        } else {
            // Pago quincenal: determinar qué quincena del mes
            if (today.getDayOfMonth() <= 15) {
                // Primera quincena: días 1-15 del mes anterior
                YearMonth previousMonth = YearMonth.from(today.minusMonths(1));
                periodStartDate = previousMonth.atDay(16);
                periodEndDate = previousMonth.atEndOfMonth();
            } else {
                // Segunda quincena: días 1-15 del mes actual
                YearMonth currentMonth = YearMonth.from(today);
                periodStartDate = currentMonth.atDay(1);
                periodEndDate = currentMonth.atDay(15);
            }
        }
        
        // Verificar si ya se procesó el pago para esta fecha
        boolean alreadyProcessed = salaryPaymentRepository
                .findFirstByEmployeeIdAndPaymentDateOrderByPaymentDateDesc(employee.getId(), today)
                .isPresent();
        
        if (alreadyProcessed) {
            log.info("Pago ya procesado para empleado {} en fecha {}", employee.getId(), today);
            return;
        }
        
        // Calcular el monto según la frecuencia
        BigDecimal amount = salary;
        if (frequency == PaymentFrequency.BIWEEKLY) {
            // Para quincenal, dividir el sueldo mensual entre 2
            amount = salary.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
        }
        
        // Verificar si hay fondos suficientes
        boolean hasFunds = balanceService.hasSufficientFunds(amount);
        SalaryPayment payment;
        
        if (hasFunds) {
            // Procesar el pago - primero guardar el payment para tener el ID
            // Crear el registro de pago primero (temporalmente en PENDING)
            payment = SalaryPayment.builder()
                    .employee(employee)
                    .amount(amount)
                    .paymentDate(today)
                    .periodStartDate(periodStartDate)
                    .periodEndDate(periodEndDate)
                    .paymentFrequency(frequency)
                    .status(PaymentStatus.PENDING)
                    .build();
            payment = salaryPaymentRepository.save(payment);
            
            try {
                // Registrar como gasto automáticamente
                expenseService.createSalaryExpense(employee, amount, today);
                
                // Registrar en el balance y crear transacción con el ID del pago
                balanceService.recordSalaryPayment(
                        amount,
                        payment.getId(),
                        employee.getName() + " " + employee.getLastName(),
                        String.format("Pago automático - Período: %s a %s", periodStartDate, periodEndDate)
                );
                
                // Actualizar el estado del pago a PAID
                payment.setStatus(PaymentStatus.PAID);
                payment.setProcessedAt(LocalDateTime.now());
                payment = salaryPaymentRepository.save(payment);
                
                // Enviar email de notificación al empleado (si tiene email configurado)
                try {
                    String employeeEmail = employee.getEmail();
                    if (employeeEmail != null && !employeeEmail.trim().isEmpty()) {
                        String period = String.format("%s a %s", 
                                periodStartDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                periodEndDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                        emailService.sendSalaryPaymentNotificationEmail(
                                employeeEmail,
                                employee.getName() + " " + employee.getLastName(),
                                amount,
                                today,
                                period
                        );
                    }
                } catch (Exception e) {
                    log.warn("Error enviando email de notificación de pago al empleado {}: {}", 
                            employee.getId(), e.getMessage());
                }
                
                log.info("Pago procesado exitosamente para empleado {}: ${} - Período: {} a {}", 
                        employee.getId(), amount, periodStartDate, periodEndDate);
            } catch (Exception e) {
                log.error("Error procesando el pago para empleado {}: {}", 
                        employee.getId(), e.getMessage(), e);
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Error al procesar el pago: " + e.getMessage());
                payment = salaryPaymentRepository.save(payment);
            }
        } else {
            String failureReason = "Fondos insuficientes. Saldo disponible: " + balanceService.getCurrentBalance().getCurrentBalance();
            log.warn("Pago pendiente para empleado {} por fondos insuficientes. Monto: {}, Saldo disponible: {}", 
                    employee.getId(), amount, balanceService.getCurrentBalance().getCurrentBalance());
            
            // Enviar alerta
            alertService.sendLowBalanceAlert(amount, balanceService.getCurrentBalance().getCurrentBalance());
            
            // Crear el registro de pago en estado PENDING
            payment = SalaryPayment.builder()
                    .employee(employee)
                    .amount(amount)
                    .paymentDate(today)
                    .periodStartDate(periodStartDate)
                    .periodEndDate(periodEndDate)
                    .paymentFrequency(frequency)
                    .status(PaymentStatus.PENDING)
                    .processedAt(null)
                    .failureReason(failureReason)
                    .build();
            payment = salaryPaymentRepository.save(payment);
        }
    }

    private int adjustPaymentDay(int paymentDay, LocalDate date) {
        YearMonth yearMonth = YearMonth.from(date);
        int daysInMonth = yearMonth.lengthOfMonth();
        return Math.min(paymentDay, daysInMonth);
    }

    public List<SalaryPaymentResponse> getPaymentsByEmployee(Long employeeId) {
        return salaryPaymentRepository.findByEmployeeIdOrderByPaymentDateDesc(employeeId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<SalaryPaymentResponse> getAllPayments(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return salaryPaymentRepository.findAll()
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        
        LocalDate start = startDate != null ? startDate : LocalDate.of(1900, 1, 1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        
        return salaryPaymentRepository.findByPaymentDateBetween(start, end)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Procesa pagos pendientes cuando hay fondos disponibles
     */
    @Transactional
    public void processPendingPayments() {
        log.info("Procesando pagos pendientes...");
        List<SalaryPayment> pendingPayments = salaryPaymentRepository.findByStatusOrderByPaymentDateAsc(PaymentStatus.PENDING);
        
        log.info("Pagos pendientes encontrados: {}", pendingPayments.size());
        
        for (SalaryPayment payment : pendingPayments) {
            try {
                processPendingPayment(payment);
            } catch (Exception e) {
                log.error("Error procesando pago pendiente {}: {}", payment.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Procesa un pago pendiente específico
     */
    @Transactional
    public void processPendingPayment(SalaryPayment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }

        BigDecimal amount = payment.getAmount();
        boolean hasFunds = balanceService.hasSufficientFunds(amount);

        if (hasFunds) {
            try {
                Employee employee = payment.getEmployee();
                
                // Registrar como gasto
                expenseService.createSalaryExpense(employee, amount, payment.getPaymentDate());
                
                // Registrar en el balance y crear transacción
                balanceService.recordSalaryPayment(
                        amount,
                        payment.getId(),
                        employee.getName() + " " + employee.getLastName(),
                        String.format("Pago automático procesado - Período: %s a %s", 
                                payment.getPeriodStartDate(), payment.getPeriodEndDate())
                );

                // Actualizar estado del pago
                payment.setStatus(PaymentStatus.PAID);
                payment.setProcessedAt(LocalDateTime.now());
                payment.setFailureReason(null);
                salaryPaymentRepository.save(payment);

                log.info("Pago pendiente {} procesado exitosamente para empleado {}", 
                        payment.getId(), employee.getId());
            } catch (Exception e) {
                log.error("Error procesando pago pendiente {}: {}", payment.getId(), e.getMessage(), e);
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Error al procesar: " + e.getMessage());
                salaryPaymentRepository.save(payment);
            }
        } else {
            log.debug("Pago pendiente {} aún no puede procesarse por fondos insuficientes", payment.getId());
        }
    }

    /**
     * Obtiene pagos pendientes
     */
    @Transactional(readOnly = true)
    public List<SalaryPaymentResponse> getPendingPayments() {
        return salaryPaymentRepository.findByStatusOrderByPaymentDateAsc(PaymentStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private SalaryPaymentResponse mapToResponse(SalaryPayment payment) {
        return SalaryPaymentResponse.builder()
                .id(payment.getId())
                .employeeId(payment.getEmployee().getId())
                .employeeName(payment.getEmployee().getName())
                .employeeLastName(payment.getEmployee().getLastName())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .periodStartDate(payment.getPeriodStartDate())
                .periodEndDate(payment.getPeriodEndDate())
                .createdAt(payment.getCreatedAt())
                .paymentFrequency(payment.getPaymentFrequency())
                .status(payment.getStatus())
                .processedAt(payment.getProcessedAt())
                .failureReason(payment.getFailureReason())
                .build();
    }
}

