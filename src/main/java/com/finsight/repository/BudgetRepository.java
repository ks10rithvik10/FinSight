package com.finsight.repository;

import com.finsight.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserId(Long id);
    List<Budget> findByUserIdAndYearAndMonth(Long id, int year, int month);
    Optional<Budget> findByIdAndUserId(Long id, Long userId);
}
