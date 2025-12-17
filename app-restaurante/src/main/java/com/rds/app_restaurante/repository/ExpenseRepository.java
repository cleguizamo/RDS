package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByExpenseDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<Expense> findByCategory(String category);
    
    // Búsqueda avanzada de gastos
    @Query("SELECT e FROM Expense e WHERE " +
           "(:description IS NULL OR LOWER(e.description) LIKE LOWER(CONCAT('%', :description, '%'))) AND " +
           "(:category IS NULL OR e.category = :category) AND " +
           "(:paymentMethod IS NULL OR e.paymentMethod = :paymentMethod) AND " +
           "(:startDate IS NULL OR e.expenseDate >= :startDate) AND " +
           "(:endDate IS NULL OR e.expenseDate <= :endDate) AND " +
           "(:minAmount IS NULL OR e.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR e.amount <= :maxAmount)")
    Page<Expense> searchExpenses(
            @Param("description") String description,
            @Param("category") String category,
            @Param("paymentMethod") String paymentMethod,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable
    );
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpensesBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.expenseDate BETWEEN :startDate AND :endDate GROUP BY e.category")
    List<Object[]> getExpensesByCategoryBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
