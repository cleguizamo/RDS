package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.OrderItemRequest;
import com.rds.app_restaurante.dto.OrderItemResponse;
import com.rds.app_restaurante.dto.OrderRequest;
import com.rds.app_restaurante.dto.OrderResponse;
import com.rds.app_restaurante.model.Order;
import com.rds.app_restaurante.model.OrderItem;
import com.rds.app_restaurante.model.Product;
import com.rds.app_restaurante.model.User;
import com.rds.app_restaurante.repository.OrderRepository;
import com.rds.app_restaurante.repository.ProductRepository;
import com.rds.app_restaurante.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

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
        order.setDate(LocalDate.now());
        order.setTime(LocalTime.now());
        order.setTotalPrice(totalPrice);
        order.setStatus(false); // Pendiente por defecto
        order.setTableNumber(orderRequest.getTableNumber());
        order.setUser(user);

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

        // Actualizar estadísticas del usuario
        user.setNumberOfOrders(user.getNumberOfOrders() + 1);
        user.setTotalSpent(user.getTotalSpent() + totalPrice);
        user.setLastOrderDate(LocalDate.now());
        // Calcular puntos: por cada 1000 pesos gastados = 1 punto
        user.setPoints((long) Math.floor(user.getTotalSpent() / 1000.0));
        userRepository.save(user);

        return mapToResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, boolean status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + id));
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        return mapToResponse(updatedOrder);
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .date(order.getDate())
                .time(order.getTime())
                .totalPrice(order.getTotalPrice())
                .status(order.isStatus())
                .tableNumber(order.getTableNumber())
                .userId(order.getUser().getId())
                .userName(order.getUser().getName() + " " + order.getUser().getLastName())
                .userEmail(order.getUser().getEmail())
                .items(items)
                .build();
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

