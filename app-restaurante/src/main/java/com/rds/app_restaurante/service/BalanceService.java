package com.rds.app_restaurante.service;

import com.rds.app_restaurante.model.Balance;
import com.rds.app_restaurante.model.Order;
import com.rds.app_restaurante.model.Delivery;
import com.rds.app_restaurante.model.Expense;
import com.rds.app_restaurante.model.Transaction;
import com.rds.app_restaurante.model.TransactionType;
import com.rds.app_restaurante.repository.BalanceRepository;
import com.rds.app_restaurante.repository.OrderRepository;
import com.rds.app_restaurante.repository.DeliveryRepository;
import com.rds.app_restaurante.repository.ExpenseRepository;
import com.rds.app_restaurante.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final ExpenseRepository expenseRepository;

    /**
     * Obtiene el balance actual o lo crea si no existe
     */
    public Balance getCurrentBalance() {
        try {
            Optional<Balance> existing = balanceRepository.findFirstByOrderByIdAsc();
            if (existing.isPresent()) {
                return existing.get();
            }
            
            // Si no existe, crear uno nuevo en una transacción separada
            return createInitialBalance();
        } catch (Exception e) {
            // Si hay un error (por ejemplo, las tablas no existen aún), crear un balance temporal en memoria
            log.warn("Error obteniendo balance, usando balance temporal: {}", e.getMessage());
            return new Balance(BigDecimal.ZERO, BigDecimal.valueOf(100000));
        }
    }
    
    /**
     * Crea el balance inicial si no existe
     */
    @Transactional
    private Balance createInitialBalance() {
        try {
            // Verificar de nuevo en caso de condición de carrera
            Optional<Balance> existing = balanceRepository.findFirstByOrderByIdAsc();
            if (existing.isPresent()) {
                return existing.get();
            }
            
            log.warn("No existe registro de balance, creando uno inicial con saldo 0");
            Balance newBalance = new Balance(BigDecimal.ZERO, BigDecimal.valueOf(100000)); // Threshold por defecto: 100,000
            return balanceRepository.save(newBalance);
        } catch (Exception e) {
            log.error("Error creando balance inicial: {}", e.getMessage(), e);
            // Retornar un balance temporal si no se puede crear en BD
            return new Balance(BigDecimal.ZERO, BigDecimal.valueOf(100000));
        }
    }

    /**
     * Inicializa el balance con un saldo inicial
     */
    @Transactional
    public Balance initializeBalance(BigDecimal initialBalance, BigDecimal lowBalanceThreshold) {
        Optional<Balance> existing = balanceRepository.findFirstByOrderByIdAsc();
        if (existing.isPresent()) {
            throw new RuntimeException("El balance ya está inicializado. Use updateBalance para modificarlo.");
        }

        Balance balance = new Balance(initialBalance, lowBalanceThreshold);
        Balance saved = balanceRepository.save(balance);

        // Crear transacción inicial
        createTransaction(
                TransactionType.ADJUSTMENT,
                initialBalance,
                BigDecimal.ZERO,
                initialBalance,
                "Inicialización del balance",
                null,
                "BALANCE",
                "Balance inicial configurado"
        );

        log.info("Balance inicializado con saldo: {}", initialBalance);
        return saved;
    }

    /**
     * Actualiza el threshold de alerta de saldo bajo
     */
    @Transactional
    public Balance updateLowBalanceThreshold(BigDecimal threshold) {
        Balance balance = getCurrentBalance();
        balance.setLowBalanceThreshold(threshold);
        return balanceRepository.save(balance);
    }

    /**
     * Registra un ingreso y actualiza el balance
     */
    @Transactional
    public Transaction recordIncome(BigDecimal amount, String description, Long referenceId, String referenceType, String notes) {
        Balance balance = getCurrentBalance();
        BigDecimal balanceBefore = balance.getCurrentBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        balance.setCurrentBalance(balanceAfter);
        balanceRepository.save(balance);

        Transaction transaction = createTransaction(
                TransactionType.INCOME,
                amount,
                balanceBefore,
                balanceAfter,
                description,
                referenceId,
                referenceType,
                notes
        );

        log.info("Ingreso registrado: {} - Balance: {} -> {}", amount, balanceBefore, balanceAfter);
        return transaction;
    }

    /**
     * Registra un gasto y actualiza el balance
     */
    @Transactional
    public Transaction recordExpense(BigDecimal amount, String description, Long referenceId, String referenceType, String notes) {
        try {
            Balance balance = getCurrentBalance();
            BigDecimal balanceBefore = balance.getCurrentBalance();
            BigDecimal balanceAfter = balanceBefore.subtract(amount);

            // Solo intentar guardar si el balance tiene un ID (está en la BD)
            if (balance.getId() != null) {
                balance.setCurrentBalance(balanceAfter);
                balanceRepository.save(balance);
            }

            Transaction transaction = createTransaction(
                    TransactionType.EXPENSE,
                    amount,
                    balanceBefore,
                    balanceAfter,
                    description,
                    referenceId,
                    referenceType,
                    notes
            );

            log.info("Gasto registrado: {} - Balance: {} -> {}", amount, balanceBefore, balanceAfter);
            return transaction;
        } catch (Exception e) {
            log.warn("No se pudo registrar el gasto en el balance (puede ser normal si las tablas aún no existen): {}", e.getMessage());
            // Crear una transacción temporal sin guardar en BD
            return Transaction.builder()
                    .transactionType(TransactionType.EXPENSE)
                    .amount(amount)
                    .description(description)
                    .balanceBefore(BigDecimal.ZERO)
                    .balanceAfter(BigDecimal.ZERO)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .notes(notes)
                    .build();
        }
    }

    /**
     * Registra un pago de sueldo y actualiza el balance
     */
    @Transactional
    public Transaction recordSalaryPayment(BigDecimal amount, Long salaryPaymentId, String employeeName, String notes) {
        return recordExpense(
                amount,
                String.format("Pago de sueldo - %s", employeeName),
                salaryPaymentId,
                "SALARY_PAYMENT",
                notes
        );
    }

    /**
     * Verifica si hay fondos suficientes para un gasto
     */
    @Transactional(readOnly = true)
    public boolean hasSufficientFunds(BigDecimal amount) {
        Balance balance = getCurrentBalance();
        return balance.getCurrentBalance().compareTo(amount) >= 0;
    }

    /**
     * Obtiene la diferencia entre el balance actual y el threshold
     * Retorna negativo si está por debajo del threshold
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalanceDifference() {
        Balance balance = getCurrentBalance();
        return balance.getCurrentBalance().subtract(balance.getLowBalanceThreshold());
    }

    /**
     * Verifica si el balance está bajo el threshold
     */
    @Transactional(readOnly = true)
    public boolean isLowBalance() {
        Balance balance = getCurrentBalance();
        return balance.getCurrentBalance().compareTo(balance.getLowBalanceThreshold()) < 0;
    }

    /**
     * Ajusta el balance manualmente (ajuste contable)
     */
    @Transactional
    public Transaction adjustBalance(BigDecimal amount, String reason, String notes) {
        Balance balance = getCurrentBalance();
        BigDecimal balanceBefore = balance.getCurrentBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount); // amount puede ser negativo

        balance.setCurrentBalance(balanceAfter);
        balanceRepository.save(balance);

        Transaction transaction = createTransaction(
                TransactionType.ADJUSTMENT,
                amount.abs(),
                balanceBefore,
                balanceAfter,
                reason != null ? reason : "Ajuste manual del balance",
                null,
                "BALANCE",
                notes
        );

        log.info("Balance ajustado: {} - Balance: {} -> {} - Razón: {}", 
                amount, balanceBefore, balanceAfter, reason);
        return transaction;
    }

    /**
     * Crea una transacción
     */
    private Transaction createTransaction(
            TransactionType type,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String description,
            Long referenceId,
            String referenceType,
            String notes
    ) {
        try {
            Transaction transaction = Transaction.builder()
                    .transactionType(type)
                    .amount(amount)
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfter)
                    .description(description)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .notes(notes)
                    .createdAt(LocalDateTime.now())
                    .build();

            return transactionRepository.save(transaction);
        } catch (Exception e) {
            log.warn("No se pudo guardar la transacción en la BD (puede ser normal si las tablas aún no existen): {}", e.getMessage());
            // Retornar transacción en memoria si no se puede guardar
            return Transaction.builder()
                    .transactionType(type)
                    .amount(amount)
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfter)
                    .description(description)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .notes(notes)
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Obtiene todas las transacciones
     */
    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Obtiene transacciones por tipo
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByType(TransactionType type) {
        return transactionRepository.findByTransactionTypeOrderByCreatedAtDesc(type);
    }

    /**
     * Obtiene transacciones por referencia
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByReference(String referenceType, Long referenceId) {
        return transactionRepository.findByReference(referenceType, referenceId);
    }

    /**
     * Elimina una transacción y recalcula el balance
     */
    @Transactional
    public void deleteTransaction(Long transactionId) {
        try {
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transacción no encontrada con ID: " + transactionId));
            
            log.info("Eliminando transacción: ID={}, Tipo={}, Monto={}", 
                    transactionId, transaction.getTransactionType(), transaction.getAmount());
            
            transactionRepository.deleteById(transactionId);
            
            // Recalcular el balance después de eliminar la transacción
            recalculateBalanceFromAllTransactions();
            
            log.info("Transacción eliminada y balance recalculado");
        } catch (Exception e) {
            log.error("Error al eliminar transacción {}: {}", transactionId, e.getMessage(), e);
            throw new RuntimeException("Error al eliminar transacción: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene transacciones por rango de fechas
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            return transactionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate);
        } catch (Exception e) {
            log.warn("Error obteniendo transacciones por rango de fechas: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Migra los pedidos y entregas históricos completados al balance
     * Registra transacciones para todos los pedidos/entregas completados que no tienen transacción
     */
    @Transactional
    public MigrationResult migrateHistoricalOrdersAndDeliveries() {
        try {
            log.info("Iniciando migración de pedidos y entregas históricos al balance...");
            
            int ordersMigrated = 0;
            int deliveriesMigrated = 0;
            BigDecimal totalOrdersRevenue = BigDecimal.ZERO;
            BigDecimal totalDeliveriesRevenue = BigDecimal.ZERO;
            
            // Obtener todos los pedidos completados
            List<Order> completedOrders = orderRepository.findByStatus(true);
            log.info("Encontrados {} pedidos completados", completedOrders.size());
            
            // Obtener todas las transacciones existentes y filtrar por tipo
            List<Transaction> allTransactions = transactionRepository.findAllByOrderByCreatedAtDesc();
            List<Transaction> existingOrderTransactions = allTransactions.stream()
                    .filter(t -> "ORDER".equals(t.getReferenceType()))
                    .collect(java.util.stream.Collectors.toList());
            
            // Para cada pedido completado, verificar si ya tiene transacción
            for (Order order : completedOrders) {
                boolean hasTransaction = existingOrderTransactions.stream()
                        .anyMatch(t -> t.getReferenceId() != null && t.getReferenceId().equals(order.getId()));
                
                if (!hasTransaction) {
                    // Registrar ingreso histórico
                    BigDecimal orderAmount = BigDecimal.valueOf(order.getTotalPrice());
                    LocalDateTime transactionDate = order.getDate().atTime(
                            order.getTime() != null ? order.getTime() : LocalTime.of(12, 0)
                    );
                    
                    Transaction savedTransaction = recordHistoricalIncome(
                            orderAmount,
                            String.format("Ingreso por pedido en mesa #%d - Mesa %d (migración histórica)", 
                                    order.getId(), order.getTableNumber()),
                            order.getId(),
                            "ORDER",
                            String.format("Pedido completado el %s (migrado)", order.getDate()),
                            transactionDate
                    );
                    
                    if (savedTransaction != null && savedTransaction.getId() != null) {
                        log.debug("Transacción de pedido guardada: ID={}, Monto={}", savedTransaction.getId(), orderAmount);
                    } else {
                        log.warn("Transacción de pedido NO se guardó correctamente: Order ID={}", order.getId());
                    }
                    
                    totalOrdersRevenue = totalOrdersRevenue.add(orderAmount);
                    ordersMigrated++;
                }
            }
            
            // Obtener todas las entregas completadas
            List<Delivery> completedDeliveries = deliveryRepository.findByStatus(true);
            log.info("Encontradas {} entregas completadas", completedDeliveries.size());
            
            // Obtener todas las entregas que ya tienen transacción
            List<Transaction> existingDeliveryTransactions = allTransactions.stream()
                    .filter(t -> "DELIVERY".equals(t.getReferenceType()))
                    .collect(java.util.stream.Collectors.toList());
            
            // Para cada entrega completada, verificar si ya tiene transacción
            for (Delivery delivery : completedDeliveries) {
                boolean hasTransaction = existingDeliveryTransactions.stream()
                        .anyMatch(t -> t.getReferenceId() != null && t.getReferenceId().equals(delivery.getId()));
                
                if (!hasTransaction) {
                    // Registrar ingreso histórico
                    BigDecimal deliveryAmount = BigDecimal.valueOf(delivery.getTotalPrice());
                    LocalDateTime transactionDate = delivery.getDate().atTime(
                            delivery.getTime() != null ? delivery.getTime() : LocalTime.of(12, 0)
                    );
                    
                    Transaction savedTransaction = recordHistoricalIncome(
                            deliveryAmount,
                            String.format("Ingreso por entrega a domicilio #%d (migración histórica)", delivery.getId()),
                            delivery.getId(),
                            "DELIVERY",
                            String.format("Entrega completada el %s - %s (migrada)", 
                                    delivery.getDate(), delivery.getDeliveryAddress()),
                            transactionDate
                    );
                    
                    if (savedTransaction != null && savedTransaction.getId() != null) {
                        log.debug("Transacción de entrega guardada: ID={}, Monto={}", savedTransaction.getId(), deliveryAmount);
                    } else {
                        log.warn("Transacción de entrega NO se guardó correctamente: Delivery ID={}", delivery.getId());
                    }
                    
                    totalDeliveriesRevenue = totalDeliveriesRevenue.add(deliveryAmount);
                    deliveriesMigrated++;
                }
            }
            
            // Migrar gastos históricos (excluyendo "Nómina" que se maneja por separado)
            int expensesMigrated = 0;
            BigDecimal totalExpenses = BigDecimal.ZERO;
            List<Expense> allExpenses = expenseRepository.findAll();
            log.info("Encontrados {} gastos en total para revisar", allExpenses.size());
            
            // Obtener todas las transacciones de gastos existentes
            List<Transaction> existingExpenseTransactions = allTransactions.stream()
                    .filter(t -> "EXPENSE".equals(t.getReferenceType()))
                    .collect(java.util.stream.Collectors.toList());
            
            // Para cada gasto, verificar si ya tiene transacción (excluyendo "Nómina")
            for (Expense expense : allExpenses) {
                // Excluir gastos de nómina ya que se manejan por SalaryPayment
                if ("Nómina".equals(expense.getCategory())) {
                    continue;
                }
                
                boolean hasTransaction = existingExpenseTransactions.stream()
                        .anyMatch(t -> t.getReferenceId() != null && t.getReferenceId().equals(expense.getId()));
                
                if (!hasTransaction) {
                    // Registrar gasto histórico
                    BigDecimal expenseAmount = expense.getAmount();
                    LocalDateTime transactionDate = expense.getExpenseDate().atTime(12, 0);
                    
                    Transaction savedTransaction = recordHistoricalExpense(
                            expenseAmount,
                            String.format("Gasto histórico: %s - %s (migración)", expense.getCategory(), expense.getDescription()),
                            expense.getId(),
                            "EXPENSE",
                            String.format("Gasto del %s - Método de pago: %s (migrado)", 
                                    expense.getExpenseDate(), 
                                    expense.getPaymentMethod() != null ? expense.getPaymentMethod() : "No especificado"),
                            transactionDate
                    );
                    
                    if (savedTransaction != null && savedTransaction.getId() != null) {
                        log.debug("Transacción de gasto guardada: ID={}, Monto={}", savedTransaction.getId(), expenseAmount);
                    } else {
                        log.warn("Transacción de gasto NO se guardó correctamente: Expense ID={}", expense.getId());
                    }
                    
                    totalExpenses = totalExpenses.add(expenseAmount);
                    expensesMigrated++;
                    
                    if (expensesMigrated <= 5) {
                        log.debug("Gasto migrado: ID={}, Categoría={}, Monto={}, Fecha={}", 
                                expense.getId(), expense.getCategory(), expenseAmount, expense.getExpenseDate());
                    }
                }
            }
            
            log.info("Migrados {} gastos históricos. Total gastos: {}", expensesMigrated, totalExpenses);
            
            // Verificar cuántas transacciones tenemos ahora en total
            List<Transaction> totalTransactions = transactionRepository.findAllByOrderByCreatedAtDesc();
            log.info("Total de transacciones después de la migración: {}", totalTransactions.size());
            
            // Recalcular el balance completo desde TODAS las transacciones (históricas y existentes)
            log.info("Recalculando balance completo desde todas las transacciones...");
            recalculateBalanceFromAllTransactions();
            
            Balance finalBalance = getCurrentBalance();
            log.info("Balance final después de la migración: {}", finalBalance.getCurrentBalance());
            
            log.info("Migración completada: {} pedidos, {} entregas y {} gastos migrados. Total ingresos: {}, Total gastos: {}, Balance neto: {}",
                    ordersMigrated, deliveriesMigrated, expensesMigrated, 
                    totalOrdersRevenue.add(totalDeliveriesRevenue), 
                    totalExpenses,
                    totalOrdersRevenue.add(totalDeliveriesRevenue).subtract(totalExpenses));
            
            return new MigrationResult(ordersMigrated, deliveriesMigrated, expensesMigrated,
                    totalOrdersRevenue, totalDeliveriesRevenue, totalExpenses);
                    
        } catch (Exception e) {
            log.error("Error durante la migración: {}", e.getMessage(), e);
            throw new RuntimeException("Error al migrar pedidos y entregas históricos: " + e.getMessage(), e);
        }
    }
    
    /**
     * Registra un ingreso histórico con una fecha específica (para migraciones)
     */
    private Transaction recordHistoricalIncome(
            BigDecimal amount,
            String description,
            Long referenceId,
            String referenceType,
            String notes,
            LocalDateTime transactionDate
    ) {
        try {
            Transaction transaction = Transaction.builder()
                    .transactionType(TransactionType.INCOME)
                    .amount(amount)
                    .balanceBefore(BigDecimal.ZERO) // Se recalculará después
                    .balanceAfter(BigDecimal.ZERO)  // Se recalculará después
                    .description(description)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .notes(notes)
                    .createdAt(transactionDate)
                    .build();
            
            return transactionRepository.save(transaction);
        } catch (Exception e) {
            log.warn("Error guardando transacción histórica para {} {}: {}", 
                    referenceType, referenceId, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Registra un gasto histórico sin actualizar el balance (se recalculará después)
     */
    private Transaction recordHistoricalExpense(
            BigDecimal amount,
            String description,
            Long referenceId,
            String referenceType,
            String notes,
            LocalDateTime transactionDate
    ) {
        try {
            Transaction transaction = Transaction.builder()
                    .transactionType(TransactionType.EXPENSE)
                    .amount(amount)
                    .balanceBefore(BigDecimal.ZERO) // Se recalculará después
                    .balanceAfter(BigDecimal.ZERO)  // Se recalculará después
                    .description(description)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .notes(notes)
                    .createdAt(transactionDate)
                    .build();
            
            return transactionRepository.save(transaction);
        } catch (Exception e) {
            log.warn("Error guardando transacción histórica de gasto para {} {}: {}", 
                    referenceType, referenceId, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Recalcula el balance completo desde todas las transacciones, ordenándolas por fecha
     * Actualiza el balanceBefore y balanceAfter de cada transacción y el balance final
     */
    @Transactional
    public void recalculateBalanceFromAllTransactions() {
        try {
            // Obtener todas las transacciones
            List<Transaction> allTransactionsDesc = transactionRepository.findAllByOrderByCreatedAtDesc();
            
            if (allTransactionsDesc.isEmpty()) {
                log.warn("No hay transacciones para recalcular. El balance se mantendrá en su valor actual.");
                return;
            }
            
            // Ordenar manualmente por fecha ascendente (más antiguas primero) para calcular desde el inicio
            List<Transaction> allTransactions = allTransactionsDesc.stream()
                    .sorted((t1, t2) -> {
                        int dateCompare = t1.getCreatedAt().compareTo(t2.getCreatedAt());
                        if (dateCompare != 0) {
                            return dateCompare;
                        }
                        // Si tienen la misma fecha, ordenar por ID para mantener consistencia
                        return Long.compare(t1.getId(), t2.getId());
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            log.info("Recalculando balance desde {} transacciones", allTransactions.size());
            if (allTransactions.size() <= 10) {
                log.info("Transacciones a procesar (primeras 10):");
                for (int i = 0; i < Math.min(10, allTransactions.size()); i++) {
                    Transaction t = allTransactions.get(i);
                    log.info("  [{}] {} - {}: {} (createdAt: {})", 
                            i+1, t.getTransactionType(), t.getDescription(), 
                            t.getAmount(), t.getCreatedAt());
                }
            }
            
            // Balance inicial: comenzar desde 0
            BigDecimal currentBalance = BigDecimal.ZERO;
            int updatedCount = 0;
            
            // Asegurar que el balance existe
            Balance balance = getCurrentBalance();
            
            // Procesar cada transacción en orden cronológico desde el principio
            for (Transaction transaction : allTransactions) {
                BigDecimal balanceBefore = currentBalance;
                
                // Calcular el nuevo balance según el tipo de transacción
                currentBalance = applyTransaction(currentBalance, transaction);
                
                BigDecimal balanceAfter = currentBalance;
                
                // Actualizar la transacción con los balances correctos
                // Siempre actualizar para asegurar que los valores sean correctos
                boolean needsUpdate = transaction.getBalanceBefore() == null || 
                                     transaction.getBalanceAfter() == null ||
                                     (transaction.getBalanceBefore() != null && transaction.getBalanceBefore().compareTo(balanceBefore) != 0) ||
                                     (transaction.getBalanceAfter() != null && transaction.getBalanceAfter().compareTo(balanceAfter) != 0);
                
                if (needsUpdate) {
                    transaction.setBalanceBefore(balanceBefore);
                    transaction.setBalanceAfter(balanceAfter);
                    transactionRepository.save(transaction);
                    updatedCount++;
                    
                    if (updatedCount <= 5) {
                        log.debug("Transacción {} actualizada: Balance {} -> {} (Tipo: {}, Monto: {})",
                                transaction.getId(), balanceBefore, balanceAfter,
                                transaction.getTransactionType(), transaction.getAmount());
                    }
                }
            }
            
            // Actualizar el balance final
            BigDecimal previousBalance = balance.getCurrentBalance();
            balance.setCurrentBalance(currentBalance);
            balanceRepository.save(balance);
            
            log.info("Balance recalculado: {} transacciones procesadas, {} actualizadas. Balance anterior: {}, Balance final: {}", 
                    allTransactions.size(), updatedCount, previousBalance, currentBalance);
                    
        } catch (Exception e) {
            log.error("Error recalculando balance: {}", e.getMessage(), e);
            throw new RuntimeException("Error al recalcular el balance: " + e.getMessage(), e);
        }
    }
    
    /**
     * Aplica una transacción al balance actual y retorna el nuevo balance
     */
    private BigDecimal applyTransaction(BigDecimal currentBalance, Transaction transaction) {
        switch (transaction.getTransactionType()) {
            case INCOME:
                return currentBalance.add(transaction.getAmount());
            case EXPENSE:
            case SALARY_PAYMENT:
                return currentBalance.subtract(transaction.getAmount());
            case ADJUSTMENT:
                // Los ajustes pueden ser positivos o negativos
                return currentBalance.add(transaction.getAmount());
            case REFUND:
                return currentBalance.add(transaction.getAmount());
            default:
                log.warn("Tipo de transacción desconocido: {}", transaction.getTransactionType());
                return currentBalance;
        }
    }
    
    /**
     * Clase de resultado para la migración
     */
    public static class MigrationResult {
        private final int ordersMigrated;
        private final int deliveriesMigrated;
        private final int expensesMigrated;
        private final BigDecimal totalOrdersRevenue;
        private final BigDecimal totalDeliveriesRevenue;
        private final BigDecimal totalExpenses;
        
        public MigrationResult(int ordersMigrated, int deliveriesMigrated, int expensesMigrated,
                              BigDecimal totalOrdersRevenue, BigDecimal totalDeliveriesRevenue, BigDecimal totalExpenses) {
            this.ordersMigrated = ordersMigrated;
            this.deliveriesMigrated = deliveriesMigrated;
            this.expensesMigrated = expensesMigrated;
            this.totalOrdersRevenue = totalOrdersRevenue;
            this.totalDeliveriesRevenue = totalDeliveriesRevenue;
            this.totalExpenses = totalExpenses;
        }
        
        public int getOrdersMigrated() { return ordersMigrated; }
        public int getDeliveriesMigrated() { return deliveriesMigrated; }
        public int getExpensesMigrated() { return expensesMigrated; }
        public BigDecimal getTotalOrdersRevenue() { return totalOrdersRevenue; }
        public BigDecimal getTotalDeliveriesRevenue() { return totalDeliveriesRevenue; }
        public BigDecimal getTotalExpenses() { return totalExpenses; }
        public BigDecimal getTotalRevenue() {
            return totalOrdersRevenue.add(totalDeliveriesRevenue);
        }
        public BigDecimal getNetBalance() {
            return getTotalRevenue().subtract(totalExpenses);
        }
    }
}

