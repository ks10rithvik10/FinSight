package com.finsight.controller;

import com.finsight.repository.BudgetRepository;
import com.finsight.repository.TransactionRepository;
import com.finsight.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;

    public AdminController(UserRepository userRepository,
                           TransactionRepository transactionRepository,
                           BudgetRepository budgetRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return Map.of(
            "users", userRepository.count(),
            "regularUsers", userRepository.countByRole("USER"),
            "transactions", transactionRepository.count(),
            "budgets", budgetRepository.count()
        );
    }
}
