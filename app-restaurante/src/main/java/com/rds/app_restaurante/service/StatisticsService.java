package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.*;
import com.rds.app_restaurante.model.User;
import com.rds.app_restaurante.model.Order;
import com.rds.app_restaurante.model.Delivery;
import com.rds.app_restaurante.model.Transaction;
import com.rds.app_restaurante.model.TransactionType;
import com.rds.app_restaurante.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Cache deshabilitado para obtener siempre datos actualizados directamente de la BD
// import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final ReservationRepository reservationRepository;
    private final ExpenseRepository expenseRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryItemRepository deliveryItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final BalanceService balanceService;

    @Transactional(readOnly = true)
    // Cache deshabilitado para obtener siempre datos actualizados de la BD
    // @Cacheable(value = "statistics", key = "'financial_' + #startDate + '_' + #endDate", unless = "#result == null")
    public FinancialStatsResponse getFinancialStats(LocalDate startDate, LocalDate endDate) {
        try {
            log.info("Calculating financial statistics from {} to {} (hoy: {})", startDate, endDate, LocalDate.now());
            // Asegurar que el rango incluye al menos hasta hoy
            LocalDate effectiveEndDate = endDate;
            LocalDate today = LocalDate.now();
            if (effectiveEndDate.isBefore(today)) {
                effectiveEndDate = today;
                log.debug("Ajustando endDate de {} a {} para incluir el día actual", endDate, effectiveEndDate);
            }
            
            // Calcular ingresos y gastos desde las transacciones del balance (fuente de verdad)
            BigDecimal totalRevenue = BigDecimal.ZERO;
            BigDecimal ordersRevenue = BigDecimal.ZERO;
            BigDecimal deliveriesRevenue = BigDecimal.ZERO;
            BigDecimal totalExpenses = BigDecimal.ZERO;
            
            try {
                if (balanceService != null) {
                    LocalDateTime startDateTime = startDate.atStartOfDay();
                    LocalDateTime endDateTime = effectiveEndDate.plusDays(1).atStartOfDay().minusSeconds(1);
                    
                    List<Transaction> transactions = balanceService.getTransactionsBetweenDates(startDateTime, endDateTime);
                    
                    for (Transaction transaction : transactions) {
                        LocalDate transactionDate = transaction.getCreatedAt().toLocalDate();
                        if (!transactionDate.isBefore(startDate) && !transactionDate.isAfter(effectiveEndDate)) {
                            if (transaction.getTransactionType() == TransactionType.INCOME) {
                                totalRevenue = totalRevenue.add(transaction.getAmount());
                                // Separar por tipo de referencia
                                if ("ORDER".equals(transaction.getReferenceType())) {
                                    ordersRevenue = ordersRevenue.add(transaction.getAmount());
                                } else if ("DELIVERY".equals(transaction.getReferenceType())) {
                                    deliveriesRevenue = deliveriesRevenue.add(transaction.getAmount());
                                }
                            } else if (transaction.getTransactionType() == TransactionType.EXPENSE || 
                                       transaction.getTransactionType() == TransactionType.SALARY_PAYMENT) {
                                totalExpenses = totalExpenses.add(transaction.getAmount());
                            }
                        }
                    }
                    
                    log.debug("Revenue desde balance: Total={}, Orders={}, Deliveries={}, Expenses={}", 
                        totalRevenue, ordersRevenue, deliveriesRevenue, totalExpenses);
                } else {
                    log.warn("BalanceService no disponible, usando métodos antiguos");
                    // Fallback a métodos antiguos si balanceService no está disponible
                    ordersRevenue = orderRepository.getTotalRevenueBetweenDates(startDate, effectiveEndDate);
                    if (ordersRevenue == null) ordersRevenue = BigDecimal.ZERO;
                    deliveriesRevenue = deliveryRepository.getTotalRevenueBetweenDates(startDate, effectiveEndDate);
                    if (deliveriesRevenue == null) deliveriesRevenue = BigDecimal.ZERO;
                    totalRevenue = ordersRevenue.add(deliveriesRevenue);
                    totalExpenses = expenseRepository.getTotalExpensesBetweenDates(startDate, effectiveEndDate);
                    if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;
                }
            } catch (Exception e) {
                log.error("Error calculando desde balance, usando métodos antiguos: {}", e.getMessage(), e);
                // Fallback a métodos antiguos en caso de error
                try {
                    ordersRevenue = orderRepository.getTotalRevenueBetweenDates(startDate, effectiveEndDate);
                    if (ordersRevenue == null) ordersRevenue = BigDecimal.ZERO;
                    deliveriesRevenue = deliveryRepository.getTotalRevenueBetweenDates(startDate, effectiveEndDate);
                    if (deliveriesRevenue == null) deliveriesRevenue = BigDecimal.ZERO;
                    totalRevenue = ordersRevenue.add(deliveriesRevenue);
                    totalExpenses = expenseRepository.getTotalExpensesBetweenDates(startDate, effectiveEndDate);
                    if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;
                } catch (Exception ex) {
                    log.error("Error en fallback de cálculo de estadísticas: {}", ex.getMessage(), ex);
                    totalRevenue = BigDecimal.ZERO;
                    ordersRevenue = BigDecimal.ZERO;
                    deliveriesRevenue = BigDecimal.ZERO;
                    totalExpenses = BigDecimal.ZERO;
                }
            }

            // Calcular ganancia neta
            BigDecimal netProfit = totalRevenue.subtract(totalExpenses);

            // Gastos por categoría
            List<CategoryExpenseResponse> expensesByCategory = new ArrayList<>();
            try {
                List<Object[]> expensesByCategoryData = expenseRepository.getExpensesByCategoryBetweenDates(startDate, effectiveEndDate);
                expensesByCategory = expensesByCategoryData.stream()
                        .map(row -> {
                            String category = (String) row[0];
                            BigDecimal amount = (BigDecimal) row[1];
                            return CategoryExpenseResponse.builder()
                                    .category(category)
                                    .totalAmount(amount)
                                    .build();
                        })
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("Error obteniendo gastos por categoría: {}", e.getMessage(), e);
                expensesByCategory = new ArrayList<>();
            }

            // Estadísticas diarias - incluir desde startDate hasta effectiveEndDate completo
            // Esto asegura que siempre incluya el día actual si está en el rango
            List<DailyStatsResponse> dailyStats = new ArrayList<>();
            try {
                dailyStats = generateDailyStatsOptimized(startDate, effectiveEndDate);
                log.debug("Daily stats generadas: {} días desde {} hasta {}", dailyStats.size(), startDate, effectiveEndDate);
            } catch (Exception e) {
                log.error("Error generando estadísticas diarias: {}", e.getMessage(), e);
                dailyStats = new ArrayList<>();
            }

            return FinancialStatsResponse.builder()
                    .totalRevenue(totalRevenue)
                    .totalExpenses(totalExpenses)
                    .netProfit(netProfit)
                    .startDate(startDate)
                    .endDate(effectiveEndDate)
                    .ordersRevenue(ordersRevenue)
                    .deliveriesRevenue(deliveriesRevenue)
                    .expensesByCategory(expensesByCategory)
                    .dailyStats(dailyStats)
                    .build();
        } catch (Exception e) {
            log.error("Error general en getFinancialStats: {}", e.getMessage(), e);
            throw new RuntimeException("Error al calcular estadísticas financieras: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    // Cache deshabilitado para obtener siempre datos actualizados de la BD
    // @Cacheable(value = "statistics", key = "'business'", unless = "#result == null")
    public BusinessStatsResponse getBusinessStats() {
        log.info("Calculating business statistics - obteniendo datos actualizados de la BD");
        // Estadísticas generales - consultar directamente de la BD cada vez
        Long totalOrders = orderRepository.count();
        Long totalDeliveries = deliveryRepository.count();
        Long totalReservations = reservationRepository.count();
        Long totalCustomers = userRepository.count();
        Long totalProducts = productRepository.count();
        
        log.info("Estadísticas de negocio: Pedidos={}, Entregas={}, Reservas={}, Clientes={}, Productos={}", 
            totalOrders, totalDeliveries, totalReservations, totalCustomers, totalProducts);

        // Productos más vendidos (últimos 30 días)
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        
        List<Object[]> topOrderProducts = orderItemRepository.getTopProductsBetweenDates(startDate, endDate);
        List<Object[]> topDeliveryProducts = deliveryItemRepository.getTopProductsBetweenDates(startDate, endDate);
        
        // Combinar y agrupar productos
        Map<Long, TopProductResponse> productMap = new HashMap<>();
        
        // Procesar productos de pedidos
        for (Object[] row : topOrderProducts) {
            Long productId = ((Number) row[0]).longValue();
            String productName = (String) row[1];
            Long quantity = ((Number) row[2]).longValue();
            BigDecimal revenue = BigDecimal.valueOf(((Number) row[3]).doubleValue());
            
            productMap.put(productId, TopProductResponse.builder()
                    .productId(productId)
                    .productName(productName)
                    .totalQuantity(quantity)
                    .totalRevenue(revenue)
                    .build());
        }
        
        // Procesar productos de entregas
        for (Object[] row : topDeliveryProducts) {
            Long productId = ((Number) row[0]).longValue();
            String productName = (String) row[1];
            Long quantity = ((Number) row[2]).longValue();
            BigDecimal revenue = BigDecimal.valueOf(((Number) row[3]).doubleValue());
            
            TopProductResponse existing = productMap.get(productId);
            if (existing != null) {
                existing.setTotalQuantity(existing.getTotalQuantity() + quantity);
                existing.setTotalRevenue(existing.getTotalRevenue().add(revenue));
            } else {
                productMap.put(productId, TopProductResponse.builder()
                        .productId(productId)
                        .productName(productName)
                        .totalQuantity(quantity)
                        .totalRevenue(revenue)
                        .build());
            }
        }
        
        List<TopProductResponse> topProducts = productMap.values().stream()
                .sorted((a, b) -> b.getTotalQuantity().compareTo(a.getTotalQuantity()))
                .limit(10)
                .collect(Collectors.toList());

        // Clientes más frecuentes - optimizado para solo obtener usuarios con pedidos
        List<User> allUsers = userRepository.findAll();
        // Filtrar y mapear en una sola operación para mejor rendimiento
        List<TopCustomerResponse> topCustomers = allUsers.stream()
                .filter(user -> user.getNumberOfOrders() > 0)
                .map(user -> TopCustomerResponse.builder()
                        .userId(user.getId())
                        .name(user.getName())
                        .lastName(user.getLastName())
                        .totalOrders(user.getNumberOfOrders())
                        .totalSpent(BigDecimal.valueOf(user.getTotalSpent()))
                        .build())
                .sorted((a, b) -> {
                    int orderCompare = Long.compare(b.getTotalOrders(), a.getTotalOrders());
                    if (orderCompare != 0) return orderCompare;
                    return b.getTotalSpent().compareTo(a.getTotalSpent());
                })
                .limit(10)
                .collect(Collectors.toList());

        // Estadísticas de hoy - usar fecha actual
        LocalDate today = LocalDate.now();
        log.debug("Calculando estadísticas de hoy: {}", today);
        DailySummaryResponse todayStats = getDailySummary(today, today);
        log.info("Estadísticas de hoy: Ingresos={}, Gastos={}, Pedidos={}, Entregas={}", 
            todayStats.getRevenue(), todayStats.getExpenses(), 
            todayStats.getOrdersCount(), todayStats.getDeliveriesCount());

        // Estadísticas del mes actual - incluir hasta hoy
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = LocalDate.now(); // Siempre incluir hasta hoy
        log.debug("Calculando estadísticas del mes: {} a {}", monthStart, monthEnd);
        MonthlySummaryResponse monthlyStats = getMonthlySummary(monthStart, monthEnd);
        log.info("Estadísticas del mes: Ingresos={}, Gastos={}, Pedidos={}, Entregas={}", 
            monthlyStats.getRevenue(), monthlyStats.getExpenses(), 
            monthlyStats.getOrdersCount(), monthlyStats.getDeliveriesCount());

        return BusinessStatsResponse.builder()
                .totalOrders(totalOrders)
                .totalDeliveries(totalDeliveries)
                .totalReservations(totalReservations)
                .totalCustomers(totalCustomers)
                .totalProducts(totalProducts)
                .topProducts(topProducts)
                .topCustomers(topCustomers)
                .todayStats(todayStats)
                .monthlyStats(monthlyStats)
                .build();
    }

    private List<DailyStatsResponse> generateDailyStats(LocalDate startDate, LocalDate endDate) {
        List<DailyStatsResponse> dailyStats = new ArrayList<>();
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            DailySummaryResponse daily = getDailySummary(current, current);
            dailyStats.add(DailyStatsResponse.builder()
                    .date(current)
                    .revenue(daily.getRevenue())
                    .expenses(daily.getExpenses())
                    .profit(daily.getProfit())
                    .ordersCount(daily.getOrdersCount())
                    .deliveriesCount(daily.getDeliveriesCount())
                    .build());
            current = current.plusDays(1);
        }
        
        return dailyStats;
    }

    // Versión optimizada que hace menos consultas a la base de datos
    private List<DailyStatsResponse> generateDailyStatsOptimized(LocalDate startDate, LocalDate endDate) {
        try {
            Map<LocalDate, DailyStatsResponse> statsMap = new HashMap<>();
        LocalDate current = startDate;
        
        // Inicializar el mapa con todas las fechas
        while (!current.isAfter(endDate)) {
            statsMap.put(current, DailyStatsResponse.builder()
                    .date(current)
                    .revenue(BigDecimal.ZERO)
                    .expenses(BigDecimal.ZERO)
                    .profit(BigDecimal.ZERO)
                    .ordersCount(0L)
                    .deliveriesCount(0L)
                    .build());
            current = current.plusDays(1);
        }
        
        // Primero, calcular ingresos y gastos desde las transacciones del balance (fuente de verdad)
        Map<LocalDate, DailyStatsResponse> tempStatsMap = new HashMap<>(statsMap);
        
        try {
            if (balanceService != null) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay().minusSeconds(1);
                
                List<Transaction> transactions = balanceService.getTransactionsBetweenDates(startDateTime, endDateTime);
                
                for (Transaction transaction : transactions) {
                    LocalDate transactionDate = transaction.getCreatedAt().toLocalDate();
                    if (!transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate)) {
                        DailyStatsResponse stat = tempStatsMap.get(transactionDate);
                        if (stat != null) {
                            BigDecimal newRevenue = stat.getRevenue();
                            BigDecimal newExpenses = stat.getExpenses();
                            
                            if (transaction.getTransactionType() == TransactionType.INCOME) {
                                newRevenue = newRevenue.add(transaction.getAmount());
                            } else if (transaction.getTransactionType() == TransactionType.EXPENSE || 
                                       transaction.getTransactionType() == TransactionType.SALARY_PAYMENT) {
                                newExpenses = newExpenses.add(transaction.getAmount());
                            }
                            
                            DailyStatsResponse updated = DailyStatsResponse.builder()
                                    .date(stat.getDate())
                                    .revenue(newRevenue)
                                    .expenses(newExpenses)
                                    .ordersCount(stat.getOrdersCount())
                                    .deliveriesCount(stat.getDeliveriesCount())
                                    .build();
                            updated.setProfit(updated.getRevenue().subtract(updated.getExpenses()));
                            tempStatsMap.put(transactionDate, updated);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error obteniendo transacciones del balance para estadísticas diarias, usando métodos antiguos: {}", e.getMessage());
        }
        
        // Obtener pedidos y entregas solo para contar (no para ingresos, ya están en balance)
        List<Order> orders = orderRepository.findByDateBetween(startDate, endDate);
        log.info("Pedidos encontrados para rango [{}, {}]: {} pedidos", startDate, endDate, orders.size());
        
        for (Order order : orders) {
            DailyStatsResponse stat = tempStatsMap.get(order.getDate());
            if (stat != null) {
                DailyStatsResponse updated = DailyStatsResponse.builder()
                        .date(stat.getDate())
                        .revenue(stat.getRevenue()) // Ya calculado desde balance
                        .expenses(stat.getExpenses()) // Ya calculado desde balance
                        .ordersCount(stat.getOrdersCount() + 1)
                        .deliveriesCount(stat.getDeliveriesCount())
                        .build();
                updated.setProfit(updated.getRevenue().subtract(updated.getExpenses()));
                tempStatsMap.put(order.getDate(), updated);
            }
        }
        
        // Obtener entregas agrupadas por fecha en una sola consulta (solo para contar)
        List<Delivery> deliveries = deliveryRepository.findByDateBetween(startDate, endDate);
        log.info("Entregas encontradas para rango [{}, {}]: {} entregas", startDate, endDate, deliveries.size());
        
        for (Delivery delivery : deliveries) {
            DailyStatsResponse stat = tempStatsMap.get(delivery.getDate());
            if (stat != null) {
                DailyStatsResponse updated = DailyStatsResponse.builder()
                        .date(stat.getDate())
                        .revenue(stat.getRevenue()) // Ya calculado desde balance
                        .expenses(stat.getExpenses()) // Ya calculado desde balance
                        .ordersCount(stat.getOrdersCount())
                        .deliveriesCount(stat.getDeliveriesCount() + 1)
                        .build();
                updated.setProfit(updated.getRevenue().subtract(updated.getExpenses()));
                tempStatsMap.put(delivery.getDate(), updated);
            }
        }
        
            return tempStatsMap.values().stream()
                    .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error en generateDailyStatsOptimized: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar estadísticas diarias: " + e.getMessage(), e);
        }
    }

    private DailySummaryResponse getDailySummary(LocalDate date, LocalDate endDate) {
        Long ordersCount = orderRepository.countOrdersBetweenDates(date, endDate);
        Long deliveriesCount = deliveryRepository.countDeliveriesBetweenDates(date, endDate);
        Long reservationsCount = reservationRepository.countReservationsBetweenDates(date, endDate);
        
        BigDecimal ordersRevenue = BigDecimal.ZERO;
        BigDecimal deliveriesRevenue = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        
        // Usar transacciones del balance como fuente de verdad
        try {
            if (balanceService != null) {
                LocalDateTime startDateTime = date.atStartOfDay();
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay().minusSeconds(1);
                
                List<Transaction> transactions = balanceService.getTransactionsBetweenDates(startDateTime, endDateTime);
                
                for (Transaction transaction : transactions) {
                    LocalDate transactionDate = transaction.getCreatedAt().toLocalDate();
                    if (!transactionDate.isBefore(date) && !transactionDate.isAfter(endDate)) {
                        if (transaction.getTransactionType() == TransactionType.INCOME) {
                            if ("ORDER".equals(transaction.getReferenceType())) {
                                ordersRevenue = ordersRevenue.add(transaction.getAmount());
                            } else if ("DELIVERY".equals(transaction.getReferenceType())) {
                                deliveriesRevenue = deliveriesRevenue.add(transaction.getAmount());
                            }
                        } else if (transaction.getTransactionType() == TransactionType.EXPENSE || 
                                   transaction.getTransactionType() == TransactionType.SALARY_PAYMENT) {
                            expenses = expenses.add(transaction.getAmount());
                        }
                    }
                }
                
                log.debug("DailySummary desde balance para [{}, {}]: Revenue={}, Expenses={}", 
                        date, endDate, ordersRevenue.add(deliveriesRevenue), expenses);
            } else {
                // Fallback a métodos antiguos
                ordersRevenue = orderRepository.getTotalRevenueBetweenDates(date, endDate);
                if (ordersRevenue == null) ordersRevenue = BigDecimal.ZERO;
                
                deliveriesRevenue = deliveryRepository.getTotalRevenueBetweenDates(date, endDate);
                if (deliveriesRevenue == null) deliveriesRevenue = BigDecimal.ZERO;
                
                expenses = expenseRepository.getTotalExpensesBetweenDates(date, endDate);
                if (expenses == null) expenses = BigDecimal.ZERO;
            }
        } catch (Exception e) {
            log.warn("Error obteniendo datos desde balance, usando métodos antiguos: {}", e.getMessage());
            // Fallback a métodos antiguos
            ordersRevenue = orderRepository.getTotalRevenueBetweenDates(date, endDate);
            if (ordersRevenue == null) ordersRevenue = BigDecimal.ZERO;
            
            deliveriesRevenue = deliveryRepository.getTotalRevenueBetweenDates(date, endDate);
            if (deliveriesRevenue == null) deliveriesRevenue = BigDecimal.ZERO;
            
            expenses = expenseRepository.getTotalExpensesBetweenDates(date, endDate);
            if (expenses == null) expenses = BigDecimal.ZERO;
        }
        
        BigDecimal revenue = ordersRevenue.add(deliveriesRevenue);
        BigDecimal profit = revenue.subtract(expenses);
        
        return DailySummaryResponse.builder()
                .ordersCount(ordersCount)
                .deliveriesCount(deliveriesCount)
                .reservationsCount(reservationsCount)
                .revenue(revenue)
                .expenses(expenses)
                .profit(profit)
                .build();
    }

    private MonthlySummaryResponse getMonthlySummary(LocalDate startDate, LocalDate endDate) {
        Long ordersCount = orderRepository.countOrdersBetweenDates(startDate, endDate);
        Long deliveriesCount = deliveryRepository.countDeliveriesBetweenDates(startDate, endDate);
        Long reservationsCount = reservationRepository.countReservationsBetweenDates(startDate, endDate);
        
        BigDecimal ordersRevenue = BigDecimal.ZERO;
        BigDecimal deliveriesRevenue = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        
        // Usar transacciones del balance como fuente de verdad
        try {
            if (balanceService != null) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay().minusSeconds(1);
                
                List<Transaction> transactions = balanceService.getTransactionsBetweenDates(startDateTime, endDateTime);
                
                for (Transaction transaction : transactions) {
                    LocalDate transactionDate = transaction.getCreatedAt().toLocalDate();
                    if (!transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate)) {
                        if (transaction.getTransactionType() == TransactionType.INCOME) {
                            if ("ORDER".equals(transaction.getReferenceType())) {
                                ordersRevenue = ordersRevenue.add(transaction.getAmount());
                            } else if ("DELIVERY".equals(transaction.getReferenceType())) {
                                deliveriesRevenue = deliveriesRevenue.add(transaction.getAmount());
                            }
                        } else if (transaction.getTransactionType() == TransactionType.EXPENSE || 
                                   transaction.getTransactionType() == TransactionType.SALARY_PAYMENT) {
                            expenses = expenses.add(transaction.getAmount());
                        }
                    }
                }
                
                log.debug("MonthlySummary desde balance para [{}, {}]: Revenue={}, Expenses={}", 
                        startDate, endDate, ordersRevenue.add(deliveriesRevenue), expenses);
            } else {
                // Fallback a métodos antiguos
                ordersRevenue = orderRepository.getTotalRevenueBetweenDates(startDate, endDate);
                if (ordersRevenue == null) ordersRevenue = BigDecimal.ZERO;
                
                deliveriesRevenue = deliveryRepository.getTotalRevenueBetweenDates(startDate, endDate);
                if (deliveriesRevenue == null) deliveriesRevenue = BigDecimal.ZERO;
                
                expenses = expenseRepository.getTotalExpensesBetweenDates(startDate, endDate);
                if (expenses == null) expenses = BigDecimal.ZERO;
            }
        } catch (Exception e) {
            log.warn("Error obteniendo datos desde balance, usando métodos antiguos: {}", e.getMessage());
            // Fallback a métodos antiguos
            ordersRevenue = orderRepository.getTotalRevenueBetweenDates(startDate, endDate);
            if (ordersRevenue == null) ordersRevenue = BigDecimal.ZERO;
            
            deliveriesRevenue = deliveryRepository.getTotalRevenueBetweenDates(startDate, endDate);
            if (deliveriesRevenue == null) deliveriesRevenue = BigDecimal.ZERO;
            
            expenses = expenseRepository.getTotalExpensesBetweenDates(startDate, endDate);
            if (expenses == null) expenses = BigDecimal.ZERO;
        }
        
        BigDecimal revenue = ordersRevenue.add(deliveriesRevenue);
        BigDecimal profit = revenue.subtract(expenses);
        
        return MonthlySummaryResponse.builder()
                .ordersCount(ordersCount)
                .deliveriesCount(deliveriesCount)
                .reservationsCount(reservationsCount)
                .revenue(revenue)
                .expenses(expenses)
                .profit(profit)
                .month(LocalDate.now().getMonthValue())
                .year(LocalDate.now().getYear())
                .build();
    }
}

