package com.finsight.service;

import com.finsight.dto.BudgetRequest;
import com.finsight.dto.GoalRequest;
import com.finsight.dto.TransactionRequest;
import com.finsight.model.AppUser;
import com.finsight.model.Budget;
import com.finsight.model.FinancialGoal;
import com.finsight.model.Transaction;
import com.finsight.repository.BudgetRepository;
import com.finsight.repository.FinancialGoalRepository;
import com.finsight.repository.TransactionRepository;
import com.finsight.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinanceService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final FinancialGoalRepository goalRepository;
    private final UserRepository userRepository;

    public FinanceService(TransactionRepository transactionRepository,
                          BudgetRepository budgetRepository,
                          FinancialGoalRepository goalRepository,
                          UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
    }

    public Map<String, Object> toTransactionDto(Transaction tx) {
        return Map.of(
            "id", tx.getId(),
            "amount", tx.getAmount(),
            "type", tx.getType(),
            "category", tx.getCategory(),
            "description", tx.getDescription() == null ? "" : tx.getDescription(),
            "transactionDate", tx.getTransactionDate().toString()
        );
    }

    public BigDecimal sumByType(List<Transaction> list, String type) {
        return list.stream()
            .filter(tx -> tx.getType().equalsIgnoreCase(type))
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Map<String, Object>> getAllTransactions(AppUser user) {
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(user.getId())
            .stream()
            .map(this::toTransactionDto)
            .toList();
    }

    public Map<String, Object> addTransaction(AppUser user, TransactionRequest req) {
        LocalDate date = req.transactionDate() == null ? LocalDate.now() : req.transactionDate();
        Transaction tx = new Transaction(user, req.amount(), req.type(), req.category().trim(), req.description(), date);
        return toTransactionDto(transactionRepository.save(tx));
    }

    public Map<String, Object> updateTransaction(AppUser user, Long id, TransactionRequest req) {
        Transaction tx = transactionRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        tx.setAmount(req.amount());
        tx.setType(req.type());
        tx.setCategory(req.category().trim());
        tx.setDescription(req.description());
        tx.setTransactionDate(req.transactionDate() == null ? LocalDate.now() : req.transactionDate());
        return toTransactionDto(transactionRepository.save(tx));
    }

    public void deleteTransaction(AppUser user, Long id) {
        Transaction tx = transactionRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        transactionRepository.delete(tx);
    }

    public Map<String, Object> getDashboardSummary(AppUser user) {
        List<Transaction> all = transactionRepository.findByUserIdOrderByTransactionDateDesc(user.getId());
        LocalDate now = LocalDate.now();

        List<Transaction> thisMonth = all.stream()
            .filter(tx -> tx.getTransactionDate().getYear() == now.getYear()
                       && tx.getTransactionDate().getMonthValue() == now.getMonthValue())
            .toList();

        BigDecimal monthIncome = sumByType(thisMonth, "INCOME");
        BigDecimal monthExpense = sumByType(thisMonth, "EXPENSE");
        BigDecimal totalIncome = sumByType(all, "INCOME");
        BigDecimal totalExpense = sumByType(all, "EXPENSE");

        return Map.of(
            "balance", totalIncome.subtract(totalExpense),
            "income", monthIncome,
            "expenses", monthExpense,
            "savings", monthIncome.subtract(monthExpense),
            "transactions", all.stream().limit(6).map(this::toTransactionDto).toList(),
            "month", now.getMonth().toString()
        );
    }

    /**
     * Connects budgets directly with matching EXPENSE transactions (by category, case-insensitive).
     * Counts all recorded expenses in that category (prioritizing current month if present, or all recorded expenses)
     * and returns the linked expense transactions so the UI shows live progress immediately.
     */
    public List<Map<String, Object>> getCurrentMonthBudgets(AppUser user) {
        List<Transaction> allUserExpenses = transactionRepository.findByUserIdOrderByTransactionDateDesc(user.getId())
            .stream()
            .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()))
            .toList();

        return budgetRepository.findByUserId(user.getId())
            .stream()
            .map(b -> {
                String targetCat = b.getCategory() == null ? "" : b.getCategory().trim();
                List<Transaction> matchedTxs = allUserExpenses.stream()
                    .filter(t -> t.getCategory() != null && t.getCategory().trim().equalsIgnoreCase(targetCat))
                    .toList();

                BigDecimal spent = matchedTxs.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                Map<String, Object> dto = new LinkedHashMap<>();
                dto.put("id", b.getId());
                dto.put("category", targetCat);
                dto.put("limitAmount", b.getLimitAmount());
                dto.put("spent", spent);
                dto.put("remaining", b.getLimitAmount().subtract(spent));
                dto.put("month", b.getMonth());
                dto.put("year", b.getYear());
                dto.put("transactionCount", matchedTxs.size());
                dto.put("transactions", matchedTxs.stream().map(this::toTransactionDto).toList());
                return dto;
            })
            .toList();
    }

    @Transactional
    public Map<String, Object> addBudget(AppUser user, BudgetRequest req) {
        String cleanCategory = req.category().trim();
        // If user already has a budget for this category, update its limit instead of throwing a duplicate SQL error
        Budget budget = budgetRepository.findByUserId(user.getId()).stream()
            .filter(b -> b.getCategory() != null && b.getCategory().trim().equalsIgnoreCase(cleanCategory))
            .findFirst()
            .orElseGet(() -> new Budget(user, cleanCategory, req.month(), req.year(), req.limitAmount()));

        budget.setCategory(cleanCategory);
        budget.setMonth(req.month());
        budget.setYear(req.year());
        budget.setLimitAmount(req.limitAmount());
        Budget saved = budgetRepository.save(budget);

        return Map.of(
            "id", saved.getId(),
            "category", saved.getCategory(),
            "limitAmount", saved.getLimitAmount(),
            "month", saved.getMonth(),
            "year", saved.getYear()
        );
    }

    @Transactional
    public void deleteBudget(AppUser user, Long id) {
        Budget b = budgetRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
        budgetRepository.delete(b);
    }

    /**
     * Connects savings goals with transactions!
     * Any transaction whose category is "Savings" (or matches the goal name) or whose description mentions
     * the goal name is automatically added to the goal's initial saved amount and displayed under that goal.
     */
    public List<Map<String, Object>> getGoals(AppUser user) {
        List<Transaction> allTxs = transactionRepository.findByUserIdOrderByTransactionDateDesc(user.getId());

        return goalRepository.findByUserId(user.getId())
            .stream()
            .map(g -> {
                String goalName = g.getName() == null ? "" : g.getName().trim();
                String goalNameLower = goalName.toLowerCase();

                List<Transaction> matchedTxs = allTxs.stream()
                    .filter(t -> {
                        String cat = t.getCategory() == null ? "" : t.getCategory().trim().toLowerCase();
                        String desc = t.getDescription() == null ? "" : t.getDescription().trim().toLowerCase();
                        boolean matchesGoalName = !goalNameLower.isEmpty()
                            && (cat.equals(goalNameLower) || desc.contains(goalNameLower));
                        boolean isSavingsCategory = cat.equals("savings") || cat.equals("saving goal");
                        return matchesGoalName || isSavingsCategory;
                    })
                    .toList();

                BigDecimal contributedFromTxs = matchedTxs.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal baseSaved = g.getCurrentAmount() == null ? BigDecimal.ZERO : g.getCurrentAmount();
                BigDecimal totalSaved = baseSaved.add(contributedFromTxs);

                Map<String, Object> dto = new LinkedHashMap<>();
                dto.put("id", g.getId());
                dto.put("name", goalName);
                dto.put("targetAmount", g.getTargetAmount());
                dto.put("baseAmount", baseSaved);
                dto.put("contributedAmount", contributedFromTxs);
                dto.put("currentAmount", totalSaved);
                dto.put("remaining", g.getTargetAmount().subtract(totalSaved));
                dto.put("targetDate", g.getTargetDate() == null ? "" : g.getTargetDate().toString());
                dto.put("transactionCount", matchedTxs.size());
                dto.put("transactions", matchedTxs.stream().map(this::toTransactionDto).toList());
                return dto;
            })
            .toList();
    }

    public Map<String, Object> addGoal(AppUser user, GoalRequest req) {
        BigDecimal current = req.currentAmount() == null ? BigDecimal.ZERO : req.currentAmount();
        FinancialGoal goal = goalRepository.save(new FinancialGoal(user, req.name().trim(), req.targetAmount(), current, req.targetDate()));
        return Map.of(
            "id", goal.getId(),
            "name", goal.getName(),
            "targetAmount", goal.getTargetAmount(),
            "currentAmount", goal.getCurrentAmount()
        );
    }

    @Transactional
    public void deleteGoal(AppUser user, Long id) {
        FinancialGoal g = goalRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found"));
        goalRepository.delete(g);
    }

    @Transactional
    public void clearAllUserData(AppUser user) {
        transactionRepository.deleteAll(transactionRepository.findByUserIdOrderByTransactionDateDesc(user.getId()));
        budgetRepository.deleteAll(budgetRepository.findByUserId(user.getId()));
        goalRepository.deleteAll(goalRepository.findByUserId(user.getId()));
    }

    @Transactional
    public void deleteUserAccount(AppUser user) {
        clearAllUserData(user);
        userRepository.delete(user);
    }
}
