package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.Expense;
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
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpensesBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.expenseDate BETWEEN :startDate AND :endDate GROUP BY e.category")
    List<Object[]> getExpensesByCategoryBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

