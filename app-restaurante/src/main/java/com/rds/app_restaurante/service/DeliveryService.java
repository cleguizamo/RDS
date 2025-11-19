package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.DeliveryRequest;
import com.rds.app_restaurante.dto.DeliveryResponse;
import com.rds.app_restaurante.dto.OrderItemRequest;
import com.rds.app_restaurante.dto.OrderItemResponse;
import com.rds.app_restaurante.model.Delivery;
import com.rds.app_restaurante.model.DeliveryItem;
import com.rds.app_restaurante.model.Product;
import com.rds.app_restaurante.model.User;
import com.rds.app_restaurante.repository.DeliveryRepository;
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
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

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
        delivery.setDate(LocalDate.now());
        delivery.setTime(LocalTime.now());
        delivery.setTotalPrice(totalPrice);
        delivery.setStatus(false); // Pendiente por defecto
        delivery.setDeliveryAddress(deliveryRequest.getDeliveryAddress());
        delivery.setDeliveryPhone(deliveryRequest.getDeliveryPhone());
        delivery.setUser(user);

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

        // Actualizar estadísticas del usuario
        user.setNumberOfOrders(user.getNumberOfOrders() + 1);
        user.setTotalSpent(user.getTotalSpent() + totalPrice);
        user.setLastOrderDate(LocalDate.now());
        userRepository.save(user);

        return mapToResponse(savedDelivery);
    }

    @Transactional
    public DeliveryResponse updateDeliveryStatus(Long id, boolean status) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Domicilio no encontrado con id: " + id));
        delivery.setStatus(status);
        Delivery updatedDelivery = deliveryRepository.save(delivery);
        return mapToResponse(updatedDelivery);
    }

    private DeliveryResponse mapToResponse(Delivery delivery) {
        List<OrderItemResponse> items = delivery.getItems().stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());

        return DeliveryResponse.builder()
                .id(delivery.getId())
                .date(delivery.getDate())
                .time(delivery.getTime())
                .totalPrice(delivery.getTotalPrice())
                .status(delivery.isStatus())
                .deliveryAddress(delivery.getDeliveryAddress())
                .deliveryPhone(delivery.getDeliveryPhone())
                .userId(delivery.getUser().getId())
                .userName(delivery.getUser().getName() + " " + delivery.getUser().getLastName())
                .userEmail(delivery.getUser().getEmail())
                .items(items)
                .build();
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

