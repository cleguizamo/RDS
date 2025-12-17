package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.SalaryPayment;
import com.rds.app_restaurante.model.Employee;
import com.rds.app_restaurante.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment, Long> {
    List<SalaryPayment> findByEmployeeIdOrderByPaymentDateDesc(Long employeeId);
    
    List<SalaryPayment> findByEmployeeOrderByPaymentDateDesc(Employee employee);
    
    Optional<SalaryPayment> findFirstByEmployeeIdAndPaymentDateOrderByPaymentDateDesc(Long employeeId, LocalDate paymentDate);
    
    List<SalaryPayment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<SalaryPayment> findByStatus(PaymentStatus status);
    
    List<SalaryPayment> findByStatusOrderByPaymentDateAsc(PaymentStatus status);
    
    List<SalaryPayment> findByEmployeeIdAndStatusOrderByPaymentDateDesc(Long employeeId, PaymentStatus status);
}

