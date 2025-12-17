package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.OrderItemRequest;
import com.rds.app_restaurante.dto.OrderItemResponse;
import com.rds.app_restaurante.dto.OrderRequest;
import com.rds.app_restaurante.dto.OrderResponse;
import com.rds.app_restaurante.model.Admin;
import com.rds.app_restaurante.model.Order;
import com.rds.app_restaurante.model.OrderItem;
import com.rds.app_restaurante.model.PaymentMethod;
import com.rds.app_restaurante.model.PaymentStatus;
import com.rds.app_restaurante.model.Product;
import com.rds.app_restaurante.model.User;
import com.rds.app_restaurante.repository.AdminRepository;
import com.rds.app_restaurante.repository.OrderRepository;
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
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AdminRepository adminRepository;
    private final BalanceService balanceService;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + id));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByDate(LocalDate date) {
        return orderRepository.findByDate(date).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "statistics", allEntries = true)
    public OrderResponse createOrder(OrderRequest orderRequest) {
        User user = userRepository.findById(orderRequest.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + orderRequest.getUserId()));

        if (orderRequest.getTableNumber() == null) {
            throw new RuntimeException("El número de mesa es obligatorio para pedidos en mesa");
        }

        // Validar productos y stock
        double totalPrice = 0.0;
        for (OrderItemRequest itemRequest : orderRequest.getItems()) {
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

        // Crear el pedido
        Order order = new Order();
        LocalDate orderDate = LocalDate.now();
        order.setDate(orderDate);
        order.setTime(LocalTime.now());
        order.setTotalPrice(totalPrice);
        order.setStatus(false); // Pendiente por defecto
        order.setTableNumber(orderRequest.getTableNumber());
        order.setUser(user);
        
        // Configurar campos de pago
        // Para pedidos en mesa, siempre es efectivo (se paga en la tienda)
        // Si no se especifica método de pago, por defecto es efectivo
        PaymentMethod paymentMethod = orderRequest.getPaymentMethod() != null 
                ? orderRequest.getPaymentMethod() 
                : PaymentMethod.CASH;
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(PaymentStatus.PENDING); // Pendiente de verificación por defecto
        order.setPaymentProofUrl(orderRequest.getPaymentProofUrl()); // URL del comprobante si se proporciona

        // Guardar el pedido primero para tener el ID
        Order savedOrder = orderRepository.save(order);

        // Crear items del pedido y actualizar stock
        for (OrderItemRequest itemRequest : orderRequest.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + itemRequest.getProductId()));
            
            // Crear el item del pedido
            OrderItem orderItem = new OrderItem(savedOrder, product, itemRequest.getQuantity());
            savedOrder.getItems().add(orderItem);
            
            // Actualizar stock del producto
            product.setStock(product.getStock() - itemRequest.getQuantity());
            productRepository.save(product);
        }

        // Guardar el pedido con los items
        savedOrder = orderRepository.save(savedOrder);

        // Actualizar solo número de pedidos y fecha (NO actualizar totalSpent ni puntos hasta que el pago sea verificado)
        user.setNumberOfOrders(user.getNumberOfOrders() + 1);
        user.setLastOrderDate(LocalDate.now());
        userRepository.save(user);

        // Enviar email de pedido recibido (pendiente de confirmación)
        try {
            emailService.sendOrderReceivedEmail(
                    user.getEmail(),
                    user.getName() + " " + user.getLastName(),
                    savedOrder.getId(),
                    BigDecimal.valueOf(savedOrder.getTotalPrice()),
                    savedOrder.getDate(),
                    savedOrder.getTime(),
                    savedOrder.getTableNumber()
            );
        } catch (Exception e) {
            log.warn("Error enviando email de pedido recibido: {}", e.getMessage());
            // No fallar la creación del pedido si falla el email
        }

        return mapToResponse(savedOrder);
    }

    @Transactional
    @CacheEvict(value = "statistics", allEntries = true)
    public OrderResponse updateOrderStatus(Long id, boolean status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + id));
        
        boolean wasCompleted = order.isStatus();
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        
        // Si el pedido se completó (cambió de false a true), enviar email de notificación
        // NOTA: El ingreso en balance y actualización de totalSpent se hace cuando se verifica el pago, no cuando se completa
        if (status && !wasCompleted) {
            // Enviar email de notificación cuando el pedido fue entregado en la mesa
            User user = updatedOrder.getUser();
            try {
                emailService.sendOrderDeliveredEmail(
                        user.getEmail(),
                        user.getName() + " " + user.getLastName(),
                        id,
                        updatedOrder.getDate(),
                        updatedOrder.getTime(),
                        updatedOrder.getTableNumber()
                );
            } catch (Exception e) {
                log.warn("Error enviando email de pedido entregado: {}", e.getMessage());
            }
        }
        
        return mapToResponse(updatedOrder);
    }

    @Transactional
    @CacheEvict(value = "statistics", allEntries = true)
    public OrderResponse verifyPayment(Long orderId, Long adminId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + orderId));
        
        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Solo se pueden verificar pagos pendientes. Estado actual: " + order.getPaymentStatus());
        }
        
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin no encontrado con id: " + adminId));
        
        order.setPaymentStatus(PaymentStatus.VERIFIED);
        order.setVerifiedBy(admin);
        order.setVerifiedAt(LocalDateTime.now());
        
        Order updatedOrder = orderRepository.save(order);
        log.info("Pago verificado para pedido ID: {} por admin ID: {}", orderId, adminId);
        
        // Actualizar estadísticas del usuario cuando el pago es verificado
        User user = updatedOrder.getUser();
        double totalPrice = updatedOrder.getTotalPrice();
        user.setTotalSpent(user.getTotalSpent() + totalPrice);
        // Calcular puntos: por cada 1000 pesos gastados = 1 punto
        user.setPoints((long) Math.floor(user.getTotalSpent() / 1000.0));
        userRepository.save(user);
        
        // Registrar ingreso en balance cuando el pago es verificado
        try {
            BigDecimal totalPriceBD = BigDecimal.valueOf(totalPrice);
            balanceService.recordIncome(
                    totalPriceBD,
                    String.format("Ingreso por pedido en mesa #%d - Mesa %d", orderId, updatedOrder.getTableNumber()),
                    orderId,
                    "ORDER",
                    String.format("Pedido #%d - Pago verificado el %s", orderId, LocalDate.now())
            );
            log.info("Ingreso registrado en balance por pedido verificado ID: {}, Monto: {}", orderId, totalPrice);
        } catch (Exception e) {
            log.warn("Error registrando ingreso en balance para pedido ID {}: {}", orderId, e.getMessage());
            // No fallar la verificación si falla el registro en balance
        }
        
        // Enviar email de confirmación de pedido cuando el pago es verificado
        try {
            emailService.sendOrderConfirmationEmail(
                    user.getEmail(),
                    user.getName() + " " + user.getLastName(),
                    updatedOrder.getId(),
                    BigDecimal.valueOf(updatedOrder.getTotalPrice()),
                    updatedOrder.getDate(),
                    updatedOrder.getTime(),
                    updatedOrder.getTableNumber()
            );
        } catch (Exception e) {
            log.warn("Error enviando email de confirmación de pedido: {}", e.getMessage());
            // No fallar la verificación si falla el email
        }
        
        return mapToResponse(updatedOrder);
    }

    @Transactional
    @CacheEvict(value = "statistics", allEntries = true)
    public OrderResponse rejectPayment(Long orderId, Long adminId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + orderId));
        
        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Solo se pueden rechazar pagos pendientes. Estado actual: " + order.getPaymentStatus());
        }
        
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin no encontrado con id: " + adminId));
        
        order.setPaymentStatus(PaymentStatus.REJECTED);
        order.setVerifiedBy(admin);
        order.setVerifiedAt(LocalDateTime.now());
        
        Order updatedOrder = orderRepository.save(order);
        log.info("Pago rechazado para pedido ID: {} por admin ID: {}. Razón: {}", orderId, adminId, reason);
        
        // TODO: Enviar email al cliente notificando que el pago fue rechazado
        
        return mapToResponse(updatedOrder);
    }

    @Transactional
    public OrderResponse updatePaymentProof(Long orderId, String paymentProofUrl) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + orderId));
        
        order.setPaymentProofUrl(paymentProofUrl);
        Order updatedOrder = orderRepository.save(order);
        log.info("Comprobante de pago actualizado para pedido ID: {}", orderId);
        
        return mapToResponse(updatedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersWithPendingPayments() {
        return orderRepository.findByPaymentStatus(PaymentStatus.PENDING).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersWithVerifiedPayments() {
        return orderRepository.findByPaymentStatus(PaymentStatus.VERIFIED).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> items = order.getItems() != null ? order.getItems().stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList()) : Collections.emptyList();

        OrderResponse.OrderResponseBuilder builder = OrderResponse.builder()
                .id(order.getId())
                .date(order.getDate())
                .time(order.getTime())
                .totalPrice(order.getTotalPrice())
                .status(order.isStatus())
                .type(com.rds.app_restaurante.model.OrderType.EN_MESA)
                .tableNumber(order.getTableNumber())
                .userId(order.getUser().getId())
                .userName(order.getUser().getName() + " " + order.getUser().getLastName())
                .userEmail(order.getUser().getEmail())
                .items(items);
        
        // Agregar información de pago - siempre incluir, incluso si es null
        builder.paymentStatus(order.getPaymentStatus());
        builder.paymentMethod(order.getPaymentMethod());
        builder.paymentProofUrl(order.getPaymentProofUrl());
        if (order.getVerifiedBy() != null) {
            builder.verifiedBy(order.getVerifiedBy().getId());
            builder.verifiedByName(order.getVerifiedBy().getName() + " " + order.getVerifiedBy().getLastName());
        }
        builder.verifiedAt(order.getVerifiedAt());
        
        return builder.build();
    }

    private OrderItemResponse mapItemToResponse(OrderItem item) {
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

