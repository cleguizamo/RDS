package com.rds.app_restaurante.service;

import com.rds.app_restaurante.model.Alert;
import com.rds.app_restaurante.model.AlertType;
import com.rds.app_restaurante.model.AlertStatus;
import com.rds.app_restaurante.repository.AlertRepository;
import com.rds.app_restaurante.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final BalanceService balanceService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    
    @Value("${app.admin.email:}")
    private String adminEmail;

    /**
     * Envía una alerta de saldo bajo
     */
    @Transactional
    public Alert sendLowBalanceAlert(BigDecimal requiredAmount, BigDecimal currentBalance) {
        String message = String.format(
                "Saldo insuficiente para procesar pago. Monto requerido: %s, Saldo disponible: %s",
                requiredAmount,
                currentBalance
        );

        Alert alert = Alert.builder()
                .alertType(AlertType.LOW_BALANCE)
                .status(AlertStatus.ACTIVE)
                .message(message)
                .severity("HIGH")
                .createdAt(LocalDateTime.now())
                .build();

        Alert saved = alertRepository.save(alert);
        log.warn("Alerta de saldo bajo enviada: {}", message);
        
        // Enviar email de alerta al administrador
        sendAlertEmailToAdmin("Alerta: Saldo Insuficiente", message, currentBalance, requiredAmount);
        
        return saved;
    }

    /**
     * Envía una alerta cuando el balance está bajo el threshold
     */
    @Transactional
    public Alert sendBalanceThresholdAlert(BigDecimal currentBalance, BigDecimal threshold) {
        String message = String.format(
                "El saldo del negocio está por debajo del umbral configurado. Saldo actual: %s, Umbral: %s",
                currentBalance,
                threshold
        );

        Alert alert = Alert.builder()
                .alertType(AlertType.BALANCE_THRESHOLD)
                .status(AlertStatus.ACTIVE)
                .message(message)
                .severity("MEDIUM")
                .createdAt(LocalDateTime.now())
                .build();

        Alert saved = alertRepository.save(alert);
        log.warn("Alerta de umbral de saldo enviada: {}", message);
        
        // Enviar email de alerta al administrador
        try {
            emailService.sendLowBalanceAlertEmail(
                    getAdminEmail(),
                    currentBalance,
                    threshold
            );
        } catch (Exception e) {
            log.warn("Error enviando email de alerta de balance bajo: {}", e.getMessage());
        }
        
        return saved;
    }

    /**
     * Envía una alerta cuando hay pagos pendientes
     */
    @Transactional
    public Alert sendPendingPaymentsAlert(int pendingCount) {
        String message = String.format(
                "Hay %d pago(s) de sueldo pendiente(s) por falta de fondos. Por favor, recargue el balance del negocio.",
                pendingCount
        );

        Alert alert = Alert.builder()
                .alertType(AlertType.PENDING_PAYMENTS)
                .status(AlertStatus.ACTIVE)
                .message(message)
                .severity("HIGH")
                .createdAt(LocalDateTime.now())
                .build();

        Alert saved = alertRepository.save(alert);
        log.warn("Alerta de pagos pendientes enviada: {}", message);
        return saved;
    }

    /**
     * Marca una alerta como resuelta
     */
    @Transactional
    public Alert resolveAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada con id: " + alertId));
        
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        
        return alertRepository.save(alert);
    }

    /**
     * Obtiene todas las alertas activas
     */
    @Transactional(readOnly = true)
    public List<Alert> getActiveAlerts() {
        return alertRepository.findByStatusOrderByCreatedAtDesc(AlertStatus.ACTIVE);
    }

    /**
     * Obtiene todas las alertas
     */
    @Transactional(readOnly = true)
    public List<Alert> getAllAlerts() {
        return alertRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Verifica y crea alertas si es necesario
     */
    @Transactional
    public void checkAndCreateAlerts() {
        // Verificar saldo bajo threshold
        if (balanceService.isLowBalance()) {
            var balance = balanceService.getCurrentBalance();
            // Solo crear alerta si no hay una activa reciente del mismo tipo
            List<Alert> existingAlerts = alertRepository.findByAlertTypeAndStatus(
                    AlertType.BALANCE_THRESHOLD, 
                    AlertStatus.ACTIVE
            );
            if (existingAlerts.isEmpty()) {
                sendBalanceThresholdAlert(balance.getCurrentBalance(), balance.getLowBalanceThreshold());
            }
        }
    }
    
    /**
     * Obtiene el email del administrador para enviar alertas
     */
    private String getAdminEmail() {
        // Primero intentar usar el email configurado en properties
        if (adminEmail != null && !adminEmail.trim().isEmpty()) {
            return adminEmail;
        }
        
        // Si no está configurado, usar email por defecto o buscar en la BD según la implementación
        // Por ahora usamos email por defecto - se puede configurar en application.yml
        return "admin@restaurante.com";
    }
    
    /**
     * Envía email de alerta genérica al administrador
     */
    private void sendAlertEmailToAdmin(String subject, String message, BigDecimal currentBalance, BigDecimal requiredAmount) {
        try {
            emailService.sendGenericEmail(
                    getAdminEmail(),
                    subject,
                    "email/low-balance-alert",
                    java.util.Map.of(
                            "currentBalance", currentBalance,
                            "threshold", requiredAmount != null ? requiredAmount : BigDecimal.ZERO,
                            "message", message
                    )
            );
        } catch (Exception e) {
            log.warn("Error enviando email de alerta al admin: {}", e.getMessage());
        }
    }
}

