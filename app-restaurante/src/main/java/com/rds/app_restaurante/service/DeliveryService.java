package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.DeliveryRequest;
import com.rds.app_restaurante.dto.DeliveryResponse;
import com.rds.app_restaurante.dto.OrderItemRequest;
import com.rds.app_restaurante.dto.OrderItemResponse;
import com.rds.app_restaurante.model.Delivery;
import com.rds.app_restaurante.model.DeliveryItem;
import com.rds.app_restaurante.model.Product;
import com.rds.app_restaurante.model.User;
import com.rds.app_restaurante.model.Admin;
import com.rds.app_restaurante.model.PaymentStatus;
import com.rds.app_restaurante.repository.AdminRepository;
import com.rds.app_restaurante.repository.DeliveryRepository;
import com.rds.app_restaurante.repository.ProductRepository;
import com.rds.app_restaurante.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final BalanceService balanceService;
    private final EmailService emailService;
    private final AdminRepository adminRepository;

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getAllDeliveries() {
        return deliveryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getDeliveryById(Long id) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Domicilio no encontrado con id: " + id));
        return mapToResponse(delivery);
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getDeliveriesByUserId(Long userId) {
        return deliveryRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getDeliveriesByDate(LocalDate date) {
        return deliveryRepository.findByDate(date).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "statistics", allEntries = true)
    public DeliveryResponse createDelivery(DeliveryRequest deliveryRequest) {
        User user = userRepository.findById(deliveryRequest.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + deliveryRequest.getUserId()));

        if (deliveryRequest.getDeliveryAddress() == null || deliveryRequest.getDeliveryAddress().trim().isEmpty()) {
            throw new RuntimeException("La dirección de entrega es obligatoria para pedidos a domicilio");
        }
        if (deliveryRequest.getDeliveryPhone() == null) {
            throw new RuntimeException("El teléfono de entrega es obligatorio para pedidos a domicilio");
        }

        // Validar productos y stock
        double totalPrice = 0.0;
        for (OrderItemRequest itemRequest : deliveryRequest.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + itemRequest.getProductId()));
            
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new RuntimeException(
                    String.format("Stock insuficiente para el producto '%s'. Stock disponible: %d, solicitado: %d",
                        product.getName(), product.getStock(), itemRequest.getQuantity())
                );
            }
            
            totalPrice += product.getPrice() * itemRequest.getQuantity();
        }

        // Crear el domicilio
        Delivery delivery = new Delivery();
        LocalDate deliveryDate = LocalDate.now();
        delivery.setDate(deliveryDate);
        delivery.setTime(LocalTime.now());
        delivery.setTotalPrice(totalPrice);
        log.info("Creando entrega a domicilio - fecha: {}, hora: {}", deliveryDate, LocalTime.now());
        delivery.setStatus(false); // Pendiente por defecto
        delivery.setDeliveryAddress(deliveryRequest.getDeliveryAddress());
        delivery.setDeliveryPhone(deliveryRequest.getDeliveryPhone());
        delivery.setUser(user);
        
        // Configurar campos de pago
        delivery.setPaymentStatus(com.rds.app_restaurante.model.PaymentStatus.PENDING);
        delivery.setPaymentMethod(deliveryRequest.getPaymentMethod());
        delivery.setPaymentProofUrl(deliveryRequest.getPaymentProofUrl());

        // Guardar el domicilio primero para tener el ID
        Delivery savedDelivery = deliveryRepository.save(delivery);

        // Crear items del domicilio y actualizar stock
        for (OrderItemRequest itemRequest : deliveryRequest.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + itemRequest.getProductId()));
            
            // Crear el item del domicilio
            DeliveryItem deliveryItem = new DeliveryItem(savedDelivery, product, itemRequest.getQuantity());
            savedDelivery.getItems().add(deliveryItem);
            
            // Actualizar stock del producto
            product.setStock(product.getStock() - itemRequest.getQuantity());
            productRepository.save(product);
        }

        // Guardar el domicilio con los items
        savedDelivery = deliveryRepository.save(savedDelivery);

        // Actualizar solo número de pedidos y fecha (NO actualizar totalSpent ni puntos hasta que el pago sea verificado)
        user.setNumberOfOrders(user.getNumberOfOrders() + 1);
        user.setLastOrderDate(LocalDate.now());
        userRepository.save(user);

        // Enviar email de domicilio recibido (pendiente de confirmación)
        try {
            emailService.sendDeliveryReceivedEmail(
                    user.getEmail(),
                    user.getName() + " " + user.getLastName(),
                    savedDelivery.getId(),
                    BigDecimal.valueOf(savedDelivery.getTotalPrice()),
                    savedDelivery.getDate(),
                    savedDelivery.getTime(),
                    savedDelivery.getDeliveryAddress()
            );
        } catch (Exception e) {
            log.warn("Error enviando email de domicilio recibido: {}", e.getMessage());
            // No fallar la creación de la entrega si falla el email
        }

        return mapToResponse(savedDelivery);
    }

    @Transactional
    @CacheEvict(value = "statistics", allEntries = true)
    public DeliveryResponse updateDeliveryStatus(Long id, boolean status) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Domicilio no encontrado con id: " + id));
        
        boolean wasCompleted = delivery.isStatus();
        delivery.setStatus(status);
        Delivery updatedDelivery = deliveryRepository.save(delivery);
        
        // Si la entrega se completó (cambió de false a true), enviar email de notificación
        // NOTA: El ingreso en balance y actualización de totalSpent se hace cuando se verifica el pago, no cuando se completa
        if (status && !wasCompleted) {
            // Enviar email de notificación cuando la entrega está en camino
            User user = updatedDelivery.getUser();
            try {
                emailService.sendGenericEmail(
                        user.getEmail(),
                        "Tu pedido está en camino - Entrega #" + id,
                        "email/delivery-on-the-way",
                        java.util.Map.of(
                                "userName", user.getName() + " " + user.getLastName(),
                                "deliveryId", id,
                                "deliveryAddress", updatedDelivery.getDeliveryAddress(),
                                "totalAmount", BigDecimal.valueOf(updatedDelivery.getTotalPrice())
                        )
                );
            } catch (Exception e) {
                log.warn("Error enviando email de entrega en camino: {}", e.getMessage());
            }
        }
        
        return mapToResponse(updatedDelivery);
    }

    private DeliveryResponse mapToResponse(Delivery delivery) {
        List<OrderItemResponse> items = delivery.getItems() != null ? delivery.getItems().stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList()) : Collections.emptyList();

        DeliveryResponse.DeliveryResponseBuilder builder = DeliveryResponse.builder()
                .id(delivery.getId())
                .date(delivery.getDate())
                .time(delivery.getTime())
                .totalPrice(delivery.getTotalPrice())
                .status(delivery.isStatus())
                .type(com.rds.app_restaurante.model.OrderType.DOMICILIO)
                .deliveryAddress(delivery.getDeliveryAddress())
                .deliveryPhone(delivery.getDeliveryPhone())
                .userId(delivery.getUser().getId())
                .userName(delivery.getUser().getName() + " " + delivery.getUser().getLastName())
                .userEmail(delivery.getUser().getEmail())
                .items(items);

        // Campos de pago - siempre incluir, incluso si es null
        builder.paymentStatus(delivery.getPaymentStatus());
        builder.paymentMethod(delivery.getPaymentMethod());
        builder.paymentProofUrl(delivery.getPaymentProofUrl());
        if (delivery.getVerifiedBy() != null) {
            builder.verifiedByAdminId(delivery.getVerifiedBy().getId());
            builder.verifiedByAdminName(delivery.getVerifiedBy().getName() + " " + delivery.getVerifiedBy().getLastName());
        }
        builder.verifiedAt(delivery.getVerifiedAt());

        return builder.build();
    }

    @Transactional
    @CacheEvict(value = "statistics", allEntries = true)
    public DeliveryResponse updatePaymentProofUrl(Long deliveryId, String paymentProofUrl) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Domicilio no encontrado con id: " + deliveryId));
        
        delivery.setPaymentProofUrl(paymentProofUrl);
        Delivery updatedDelivery = deliveryRepository.save(delivery);
        log.info("Comprobante de pago actualizado para domicilio ID: {}", deliveryId);
        
        return mapToResponse(updatedDelivery);
    }

    @Transactional
    @CacheEvict(value = "statistics", allEntries = true)
    public DeliveryResponse verifyPayment(Long deliveryId, Long adminId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Domicilio no encontrado con id: " + deliveryId));
        
        if (delivery.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Solo se pueden verificar pagos pendientes. Estado actual: " + delivery.getPaymentStatus());
        }
        
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin no encontrado con id: " + adminId));
        
        delivery.setPaymentStatus(PaymentStatus.VERIFIED);
        delivery.setVerifiedBy(admin);
        delivery.setVerifiedAt(LocalDateTime.now());
        
        Delivery updatedDelivery = deliveryRepository.save(delivery);
        log.info("Pago verificado para domicilio ID: {} por admin ID: {}", deliveryId, adminId);
        
        // Actualizar estadísticas del usuario cuando el pago es verificado
        User user = updatedDelivery.getUser();
        double totalPrice = updatedDelivery.getTotalPrice();
        user.setTotalSpent(user.getTotalSpent() + totalPrice);
        // Calcular puntos: por cada 1000 pesos gastados = 1 punto
        user.setPoints((long) Math.floor(user.getTotalSpent() / 1000.0));
        userRepository.save(user);
        
        // Registrar ingreso en balance cuando el pago es verificado
        try {
            BigDecimal totalPriceBD = BigDecimal.valueOf(totalPrice);
            balanceService.recordIncome(
                    totalPriceBD,
                    String.format("Ingreso por domicilio #%d - Dirección: %s", deliveryId, updatedDelivery.getDeliveryAddress()),
                    deliveryId,
                    "DELIVERY",
                    String.format("Domicilio #%d - Pago verificado el %s", deliveryId, LocalDate.now())
            );
            log.info("Ingreso registrado en balance por domicilio verificado ID: {}, Monto: {}", deliveryId, totalPrice);
        } catch (Exception e) {
            log.warn("Error registrando ingreso en balance para domicilio ID {}: {}", deliveryId, e.getMessage());
            // No fallar la verificación si falla el registro en balance
        }
        
        // Enviar email de confirmación de entrega cuando el pago es verificado
        try {
            emailService.sendDeliveryConfirmationEmail(
                    user.getEmail(),
                    user.getName() + " " + user.getLastName(),
                    updatedDelivery.getId(),
                    BigDecimal.valueOf(updatedDelivery.getTotalPrice()),
                    updatedDelivery.getDate(),
                    updatedDelivery.getTime(),
                    updatedDelivery.getDeliveryAddress()
            );
        } catch (Exception e) {
            log.warn("Error enviando email de confirmación de entrega: {}", e.getMessage());
            // No fallar la verificación si falla el email
        }
        
        return mapToResponse(updatedDelivery);
    }

    @Transactional
    @CacheEvict(value = "statistics", allEntries = true)
    public DeliveryResponse rejectPayment(Long deliveryId, Long adminId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Domicilio no encontrado con id: " + deliveryId));
        
        if (delivery.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Solo se pueden rechazar pagos pendientes. Estado actual: " + delivery.getPaymentStatus());
        }
        
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin no encontrado con id: " + adminId));
        
        delivery.setPaymentStatus(PaymentStatus.REJECTED);
        delivery.setVerifiedBy(admin);
        delivery.setVerifiedAt(LocalDateTime.now());
        
        Delivery updatedDelivery = deliveryRepository.save(delivery);
        log.info("Pago rechazado para domicilio ID: {} por admin ID: {}", deliveryId, adminId);
        
        return mapToResponse(updatedDelivery);
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getDeliveriesByPaymentStatus(PaymentStatus paymentStatus) {
        return deliveryRepository.findByPaymentStatus(paymentStatus).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private OrderItemResponse mapItemToResponse(DeliveryItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productPrice(item.getProduct().getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }
}

