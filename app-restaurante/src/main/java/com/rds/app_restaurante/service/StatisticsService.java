package com.rds.app_restaurante.service;

import com.rds.app_restaurante.dto.*;
import com.rds.app_restaurante.model.User;
import com.rds.app_restaurante.model.Order;
import com.rds.app_restaurante.model.Delivery;
import com.rds.app_restaurante.model.Expense;
import com.rds.app_restaurante.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final ReservationRepository reservationRepository;
    private final ExpenseRepository expenseRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryItemRepository deliveryItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public FinancialStatsResponse getFinancialStats(LocalDate startDate, LocalDate endDate) {
        // Calcular ingresos de pedidos
        BigDecimal ordersRevenue = orderRepository.getTotalRevenueBetweenDates(startDate, endDate);
        if (ordersRevenue == null) ordersRevenue = BigDecimal.ZERO;

        // Calcular ingresos de entregas
        BigDecimal deliveriesRevenue = deliveryRepository.getTotalRevenueBetweenDates(startDate, endDate);
        if (deliveriesRevenue == null) deliveriesRevenue = BigDecimal.ZERO;

        BigDecimal totalRevenue = ordersRevenue.add(deliveriesRevenue);

        // Calcular gastos
        BigDecimal totalExpenses = expenseRepository.getTotalExpensesBetweenDates(startDate, endDate);
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

        // Calcular ganancia neta
        BigDecimal netProfit = totalRevenue.subtract(totalExpenses);

        // Gastos por categoría
        List<Object[]> expensesByCategoryData = expenseRepository.getExpensesByCategoryBetweenDates(startDate, endDate);
        List<CategoryExpenseResponse> expensesByCategory = expensesByCategoryData.stream()
                .map(row -> {
                    String category = (String) row[0];
                    BigDecimal amount = (BigDecimal) row[1];
                    return CategoryExpenseResponse.builder()
                            .category(category)
                            .totalAmount(amount)
                            .build();
                })
                .collect(Collectors.toList());

        // Estadísticas diarias - solo últimos 14 días para mejor rendimiento
        LocalDate statsStartDate = endDate.minusDays(14);
        if (statsStartDate.isBefore(startDate)) {
            statsStartDate = startDate;
        }
        List<DailyStatsResponse> dailyStats = generateDailyStatsOptimized(statsStartDate, endDate);

        return FinancialStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalExpenses(totalExpenses)
                .netProfit(netProfit)
                .startDate(startDate)
                .endDate(endDate)
                .ordersRevenue(ordersRevenue)
                .deliveriesRevenue(deliveriesRevenue)
                .expensesByCategory(expensesByCategory)
                .dailyStats(dailyStats)
                .build();
    }

    @Transactional(readOnly = true)
    public BusinessStatsResponse getBusinessStats() {
        // Estadísticas generales
        Long totalOrders = orderRepository.count();
        Long totalDeliveries = deliveryRepository.count();
        Long totalReservations = reservationRepository.count();
        Long totalCustomers = userRepository.count();
        Long totalProducts = productRepository.count();

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

        // Estadísticas de hoy
        LocalDate today = LocalDate.now();
        DailySummaryResponse todayStats = getDailySummary(today, today);

        // Estadísticas del mes actual
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = LocalDate.now();
        MonthlySummaryResponse monthlyStats = getMonthlySummary(monthStart, monthEnd);

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
        
        // Obtener pedidos agrupados por fecha en una sola consulta
        List<Order> orders = orderRepository.findByDateBetween(startDate, endDate);
        Map<LocalDate, DailyStatsResponse> tempStatsMap = new HashMap<>(statsMap);
        
        for (Order order : orders) {
            DailyStatsResponse stat = tempStatsMap.get(order.getDate());
            if (stat != null) {
                DailyStatsResponse updated = DailyStatsResponse.builder()
                        .date(stat.getDate())
                        .revenue(order.isStatus() ? stat.getRevenue().add(BigDecimal.valueOf(order.getTotalPrice())) : stat.getRevenue())
                        .expenses(stat.getExpenses())
                        .ordersCount(stat.getOrdersCount() + 1)
                        .deliveriesCount(stat.getDeliveriesCount())
                        .build();
                updated.setProfit(updated.getRevenue().subtract(updated.getExpenses()));
                tempStatsMap.put(order.getDate(), updated);
            }
        }
        
        // Obtener entregas agrupadas por fecha en una sola consulta
        List<Delivery> deliveries = deliveryRepository.findByDateBetween(startDate, endDate);
        for (Delivery delivery : deliveries) {
            DailyStatsResponse stat = tempStatsMap.get(delivery.getDate());
            if (stat != null) {
                DailyStatsResponse updated = DailyStatsResponse.builder()
                        .date(stat.getDate())
                        .revenue(delivery.isStatus() ? stat.getRevenue().add(BigDecimal.valueOf(delivery.getTotalPrice())) : stat.getRevenue())
                        .expenses(stat.getExpenses())
                        .ordersCount(stat.getOrdersCount())
                        .deliveriesCount(stat.getDeliveriesCount() + 1)
                        .build();
                updated.setProfit(updated.getRevenue().subtract(updated.getExpenses()));
                tempStatsMap.put(delivery.getDate(), updated);
            }
        }
        
        // Obtener gastos agrupados por fecha en una sola consulta
        List<Expense> expenses = expenseRepository.findByExpenseDateBetween(startDate, endDate);
        for (Expense expense : expenses) {
            DailyStatsResponse stat = tempStatsMap.get(expense.getExpenseDate());
            if (stat != null) {
                DailyStatsResponse updated = DailyStatsResponse.builder()
                        .date(stat.getDate())
                        .revenue(stat.getRevenue())
                        .expenses(stat.getExpenses().add(expense.getAmount()))
                        .ordersCount(stat.getOrdersCount())
                        .deliveriesCount(stat.getDeliveriesCount())
                        .build();
                updated.setProfit(updated.getRevenue().subtract(updated.getExpenses()));
                tempStatsMap.put(expense.getExpenseDate(), updated);
            }
        }
        
        return tempStatsMap.values().stream()
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
    }

    private DailySummaryResponse getDailySummary(LocalDate date, LocalDate endDate) {
        Long ordersCount = orderRepository.countOrdersBetweenDates(date, endDate);
        Long deliveriesCount = deliveryRepository.countDeliveriesBetweenDates(date, endDate);
        Long reservationsCount = reservationRepository.countReservationsBetweenDates(date, endDate);
        
        BigDecimal ordersRevenue = orderRepository.getTotalRevenueBetweenDates(date, endDate);
        if (ordersRevenue == null) ordersRevenue = BigDecimal.ZERO;
        
        BigDecimal deliveriesRevenue = deliveryRepository.getTotalRevenueBetweenDates(date, endDate);
        if (deliveriesRevenue == null) deliveriesRevenue = BigDecimal.ZERO;
        
        BigDecimal revenue = ordersRevenue.add(deliveriesRevenue);
        BigDecimal expenses = expenseRepository.getTotalExpensesBetweenDates(date, endDate);
        if (expenses == null) expenses = BigDecimal.ZERO;
        
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
        
        BigDecimal ordersRevenue = orderRepository.getTotalRevenueBetweenDates(startDate, endDate);
        if (ordersRevenue == null) ordersRevenue = BigDecimal.ZERO;
        
        BigDecimal deliveriesRevenue = deliveryRepository.getTotalRevenueBetweenDates(startDate, endDate);
        if (deliveriesRevenue == null) deliveriesRevenue = BigDecimal.ZERO;
        
        BigDecimal revenue = ordersRevenue.add(deliveriesRevenue);
        BigDecimal expenses = expenseRepository.getTotalExpensesBetweenDates(startDate, endDate);
        if (expenses == null) expenses = BigDecimal.ZERO;
        
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

