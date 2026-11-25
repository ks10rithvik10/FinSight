package com.finsight.repository;

import com.finsight.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);
    List<Transaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(Long id, LocalDate from, LocalDate to);
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);
    List<Transaction> findByUserIdAndTypeAndTransactionDateBetween(Long id, String type, LocalDate from, LocalDate to);
}
