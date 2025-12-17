package com.rds.app_restaurante.scheduler;

import com.rds.app_restaurante.service.AlertService;
import com.rds.app_restaurante.service.SalaryPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SalaryPaymentScheduler {

    private final SalaryPaymentService salaryPaymentService;
    private final AlertService alertService;

    // Ejecutar cada hora para verificar y procesar sueldos programados para hoy
    // Si hoy es el día de pago de algún empleado, se procesará automáticamente
    @Scheduled(cron = "0 0 * * * ?")
    public void processSalaryPayments() {
        log.info("Ejecutando verificación de sueldos programados para hoy...");
        try {
            // Procesar sueldos de hoy (solo procesará si hoy es el día de pago)
            salaryPaymentService.processSalaryPayments();
            
            // Procesar pagos pendientes después de los nuevos pagos
            salaryPaymentService.processPendingPayments();
            
            // Verificar y crear alertas
            alertService.checkAndCreateAlerts();
        } catch (Exception e) {
            log.error("Error en la tarea programada de procesamiento de sueldos: {}", e.getMessage(), e);
        }
    }

    // Ejecutar cada hora para procesar pagos pendientes (método adicional por si acaso)
    @Scheduled(cron = "0 30 * * * ?") // Media hora después del anterior para no solaparse
    public void processPendingPaymentsHourly() {
        log.info("Ejecutando verificación de pagos pendientes...");
        try {
            salaryPaymentService.processPendingPayments();
            alertService.checkAndCreateAlerts();
        } catch (Exception e) {
            log.error("Error procesando pagos pendientes: {}", e.getMessage(), e);
        }
    }
}



