package com.ofss.transaction.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ofss.transaction.entity.Transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    // Complete history: includes SUCCESS and FAILED transactions.
    List<Transaction>
    findAllByOrderByTransactionDateDescTransactionIdDesc();

    // Total successful purchases or payments within a date range.
    @Query(value = """
            SELECT COALESCE(SUM(AMOUNT), 0)
            FROM TRANSACTION
            WHERE TRANSACTION_TYPE = :type
              AND STATUS = 'SUCCESS'
              AND TRANSACTION_DATE >= :start
              AND TRANSACTION_DATE < :end
            """, nativeQuery = true)
    BigDecimal totalForPeriod(
            @Param("type") String type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Average amount across all successful purchases.
    @Query(value = """
            SELECT COALESCE(ROUND(AVG(AMOUNT), 2), 0)
            FROM TRANSACTION
            WHERE TRANSACTION_TYPE = 'PURCHASE'
              AND STATUS = 'SUCCESS'
            """, nativeQuery = true)
    BigDecimal averageSuccessfulPurchase();

    // Largest purchase; lowest transaction ID wins if amounts tie.
    Optional<Transaction>
    findFirstByTransactionTypeAndStatusOrderByAmountDescTransactionIdAsc(
            String transactionType,
            String status
    );
}