package com.rds.app_restaurante.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromEmail;
    private final String fromName;
    private final boolean mailEnabled;

    public EmailService(
            JavaMailSender mailSender, 
            TemplateEngine templateEngine,
            @Value("${spring.mail.from:noreply@restaurante.com}") String fromEmail,
            @Value("${spring.mail.from-name:Restaurante App}") String fromName,
            @Value("${spring.mail.host:}") String smtpHost) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        // Solo habilitar email si hay configuración SMTP completa
        boolean hasHost = smtpHost != null && !smtpHost.trim().isEmpty();
        this.mailEnabled = hasHost;
        
        if (!mailEnabled) {
            log.warn("Servicio de email deshabilitado: no hay configuración SMTP (SMTP_HOST vacío o no configurado). " +
                    "Los emails no se enviarán, pero la aplicación funcionará normalmente.");
        } else {
            log.info("Servicio de email habilitado con host: {}", smtpHost);
        }
    }

    /**
     * Envía un email indicando que el pedido fue recibido (pendiente de confirmación)
     */
    public void sendOrderReceivedEmail(String toEmail, String userName, Long orderId, 
                                         BigDecimal totalAmount, LocalDate orderDate, 
                                         LocalTime orderTime, Integer tableNumber) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("orderId", orderId);
            context.setVariable("totalAmount", totalAmount);
            context.setVariable("orderDate", orderDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            context.setVariable("orderTime", orderTime.format(DateTimeFormatter.ofPattern("HH:mm")));
            context.setVariable("tableNumber", tableNumber);
            
            String htmlContent = templateEngine.process("email/order-received", context);
            sendEmail(toEmail, "Pedido Recibido #" + orderId, htmlContent);
            
            log.info("Email de pedido recibido enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("Error enviando email de pedido recibido a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Envía un email de confirmación de pedido (cuando el pago ha sido verificado)
     */
    public void sendOrderConfirmationEmail(String toEmail, String userName, Long orderId, 
                                         BigDecimal totalAmount, LocalDate orderDate, 
                                         LocalTime orderTime, Integer tableNumber) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("orderId", orderId);
            context.setVariable("totalAmount", totalAmount);
            context.setVariable("orderDate", orderDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            context.setVariable("orderTime", orderTime.format(DateTimeFormatter.ofPattern("HH:mm")));
            context.setVariable("tableNumber", tableNumber);
            
            String htmlContent = templateEngine.process("email/order-confirmation", context);
            sendEmail(toEmail, "Confirmación de Pedido #" + orderId, htmlContent);
            
            log.info("Email de confirmación de pedido enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("Error enviando email de confirmación de pedido a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Envía un email indicando que el domicilio fue recibido (pendiente de confirmación)
     */
    public void sendDeliveryReceivedEmail(String toEmail, String userName, Long deliveryId,
                                            BigDecimal totalAmount, LocalDate deliveryDate,
                                            LocalTime deliveryTime, String deliveryAddress) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("deliveryId", deliveryId);
            context.setVariable("totalAmount", totalAmount);
            context.setVariable("deliveryDate", deliveryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            context.setVariable("deliveryTime", deliveryTime.format(DateTimeFormatter.ofPattern("HH:mm")));
            context.setVariable("deliveryAddress", deliveryAddress);
            
            String htmlContent = templateEngine.process("email/delivery-received", context);
            sendEmail(toEmail, "Domicilio Recibido #" + deliveryId, htmlContent);
            
            log.info("Email de domicilio recibido enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("Error enviando email de domicilio recibido a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Envía un email de confirmación de entrega (cuando el pago ha sido verificado)
     */
    public void sendDeliveryConfirmationEmail(String toEmail, String userName, Long deliveryId,
                                            BigDecimal totalAmount, LocalDate deliveryDate,
                                            LocalTime deliveryTime, String deliveryAddress) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("deliveryId", deliveryId);
            context.setVariable("totalAmount", totalAmount);
            context.setVariable("deliveryDate", deliveryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            context.setVariable("deliveryTime", deliveryTime.format(DateTimeFormatter.ofPattern("HH:mm")));
            context.setVariable("deliveryAddress", deliveryAddress);
            
            String htmlContent = templateEngine.process("email/delivery-confirmation", context);
            sendEmail(toEmail, "Confirmación de Entrega #" + deliveryId, htmlContent);
            
            log.info("Email de confirmación de entrega enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("Error enviando email de confirmación de entrega a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Envía un email indicando que el pedido fue entregado en la mesa
     */
    public void sendOrderDeliveredEmail(String toEmail, String userName, Long orderId,
                                       LocalDate orderDate, LocalTime orderTime, Integer tableNumber) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("orderId", orderId);
            context.setVariable("orderDate", orderDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            context.setVariable("orderTime", orderTime.format(DateTimeFormatter.ofPattern("HH:mm")));
            context.setVariable("tableNumber", tableNumber);
            
            String htmlContent = templateEngine.process("email/order-delivered", context);
            sendEmail(toEmail, "Pedido Entregado en Mesa #" + tableNumber + " - Pedido #" + orderId, htmlContent);
            
            log.info("Email de pedido entregado enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("Error enviando email de pedido entregado a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Envía un email indicando que la reserva fue recibida y está pendiente de confirmación
     */
    public void sendReservationPendingEmail(String toEmail, String userName, Long reservationId,
                                            LocalDate reservationDate, LocalTime reservationTime,
                                            Integer numberOfPeople, String notes) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("reservationId", reservationId);
            context.setVariable("reservationDate", reservationDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            context.setVariable("reservationTime", reservationTime.format(DateTimeFormatter.ofPattern("HH:mm")));
            context.setVariable("numberOfPeople", numberOfPeople);
            context.setVariable("notes", notes);
            
            String htmlContent = templateEngine.process("email/reservation-pending", context);
            sendEmail(toEmail, "Reserva Recibida #" + reservationId, htmlContent);
            
            log.info("Email de reserva pendiente enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("Error enviando email de reserva pendiente a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Envía un email de confirmación de reserva (solo cuando el admin confirma la reserva)
     */
    public void sendReservationConfirmationEmail(String toEmail, String userName, Long reservationId,
                                               LocalDate reservationDate, LocalTime reservationTime,
                                               Integer numberOfPeople, String notes) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("reservationId", reservationId);
            context.setVariable("reservationDate", reservationDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            context.setVariable("reservationTime", reservationTime.format(DateTimeFormatter.ofPattern("HH:mm")));
            context.setVariable("numberOfPeople", numberOfPeople);
            context.setVariable("notes", notes);
            
            String htmlContent = templateEngine.process("email/reservation-confirmation", context);
            sendEmail(toEmail, "Confirmación de Reserva #" + reservationId, htmlContent);
            
            log.info("Email de confirmación de reserva enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("Error enviando email de confirmación de reserva a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Envía un email de notificación de pago de sueldo
     */
    public void sendSalaryPaymentNotificationEmail(String toEmail, String employeeName, BigDecimal amount,
                                                  LocalDate paymentDate, String period) {
        try {
            Context context = new Context();
            context.setVariable("employeeName", employeeName);
            context.setVariable("amount", amount);
            context.setVariable("paymentDate", paymentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            context.setVariable("period", period);
            
            String htmlContent = templateEngine.process("email/salary-payment", context);
            sendEmail(toEmail, "Notificación de Pago de Sueldo", htmlContent);
            
            log.info("Email de notificación de pago de sueldo enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("Error enviando email de notificación de pago de sueldo a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Envía un email de alerta de balance bajo
     */
    public void sendLowBalanceAlertEmail(String toEmail, BigDecimal currentBalance, BigDecimal threshold) {
        try {
            Context context = new Context();
            context.setVariable("currentBalance", currentBalance);
            context.setVariable("threshold", threshold);
            
            String htmlContent = templateEngine.process("email/low-balance-alert", context);
            sendEmail(toEmail, "Alerta: Balance Bajo", htmlContent);
            
            log.info("Email de alerta de balance bajo enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("Error enviando email de alerta de balance bajo a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Envía un email de recuperación de contraseña con código de 8 dígitos
     */
    public void sendPasswordResetEmail(String toEmail, String userName, String resetCode) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("resetCode", resetCode);
            
            String htmlContent = templateEngine.process("email/password-reset", context);
            sendEmail(toEmail, "Código de Recuperación de Contraseña", htmlContent);
            
            log.info("Email de recuperación de contraseña enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("Error enviando email de recuperación de contraseña a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Envía un email genérico con template personalizado
     */
    public void sendGenericEmail(String toEmail, String subject, String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            if (variables != null) {
                variables.forEach(context::setVariable);
            }
            
            String htmlContent = templateEngine.process(templateName, context);
            sendEmail(toEmail, subject, htmlContent);
            
            log.info("Email genérico enviado a: {} con template: {}", toEmail, templateName);
        } catch (Exception e) {
            log.error("Error enviando email genérico a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Método auxiliar para enviar emails
     * No lanza excepciones para evitar que falle la aplicación si el email no se puede enviar
     */
    private void sendEmail(String to, String subject, String htmlContent) {
        if (!mailEnabled) {
            log.debug("Email no enviado (servicio deshabilitado) a: {} - Asunto: {}", to, subject);
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.debug("Email enviado exitosamente a: {}", to);
        } catch (org.springframework.mail.MailAuthenticationException e) {
            log.warn("Error de autenticación al enviar email a {}: {}. Verifica la configuración SMTP. " +
                    "Para Gmail, asegúrate de usar una contraseña de aplicación sin espacios.", 
                    to, e.getMessage());
        } catch (org.springframework.mail.MailSendException e) {
            log.warn("Error de conexión al enviar email a {}: {}. El servidor SMTP puede no estar accesible.", 
                    to, e.getMessage());
        } catch (Exception e) {
            log.warn("Error enviando email a {}: {}", to, e.getMessage());
        }
    }
}

