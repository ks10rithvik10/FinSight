package com.finsight.controller;

import com.finsight.dto.BudgetRequest;
import com.finsight.dto.GoalRequest;
import com.finsight.dto.TransactionRequest;
import com.finsight.service.AuthService;
import com.finsight.service.FinanceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FinanceController {

    private final AuthService authService;
    private final FinanceService financeService;

    public FinanceController(AuthService authService, FinanceService financeService) {
        this.authService = authService;
        this.financeService = financeService;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Authentication auth) {
        return financeService.getDashboardSummary(authService.getCurrentUser(auth));
    }

    @GetMapping("/transactions")
    public List<Map<String, Object>> getTransactions(Authentication auth) {
        return financeService.getAllTransactions(authService.getCurrentUser(auth));
    }

    @PostMapping("/transactions")
    public Map<String, Object> addTransaction(@Valid @RequestBody TransactionRequest req, Authentication auth) {
        return financeService.addTransaction(authService.getCurrentUser(auth), req);
    }

    @PutMapping("/transactions/{id}")
    public Map<String, Object> updateTransaction(@PathVariable Long id,
                                                 @Valid @RequestBody TransactionRequest req,
                                                 Authentication auth) {
        return financeService.updateTransaction(authService.getCurrentUser(auth), id, req);
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id, Authentication auth) {
        financeService.deleteTransaction(authService.getCurrentUser(auth), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/budgets")
    public List<Map<String, Object>> getBudgets(Authentication auth) {
        return financeService.getCurrentMonthBudgets(authService.getCurrentUser(auth));
    }

    @PostMapping("/budgets")
    public Map<String, Object> addBudget(@Valid @RequestBody BudgetRequest req, Authentication auth) {
        return financeService.addBudget(authService.getCurrentUser(auth), req);
    }

    @DeleteMapping("/budgets/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id, Authentication auth) {
        financeService.deleteBudget(authService.getCurrentUser(auth), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/goals")
    public List<Map<String, Object>> getGoals(Authentication auth) {
        return financeService.getGoals(authService.getCurrentUser(auth));
    }

    @PostMapping("/goals")
    public Map<String, Object> addGoal(@Valid @RequestBody GoalRequest req, Authentication auth) {
        return financeService.addGoal(authService.getCurrentUser(auth), req);
    }

    @DeleteMapping("/goals/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id, Authentication auth) {
        financeService.deleteGoal(authService.getCurrentUser(auth), id);
        return ResponseEntity.noContent().build();
    }
}
