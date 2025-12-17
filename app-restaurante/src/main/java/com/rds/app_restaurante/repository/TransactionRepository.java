package com.rds.app_restaurante.repository;

import com.rds.app_restaurante.model.Transaction;
import com.rds.app_restaurante.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByOrderByCreatedAtDesc();
    
    Page<Transaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    List<Transaction> findByTransactionTypeOrderByCreatedAtDesc(TransactionType transactionType);
    
    List<Transaction> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT t FROM Transaction t WHERE t.referenceType = :referenceType AND t.referenceId = :referenceId")
    List<Transaction> findByReference(@Param("referenceType") String referenceType, @Param("referenceId") Long referenceId);
}

