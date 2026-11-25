package com.finsight.service;

import com.finsight.model.AppUser;
import com.finsight.model.Transaction;
import com.finsight.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final FinanceService financeService;

    public AnalyticsService(TransactionRepository transactionRepository, FinanceService financeService) {
        this.transactionRepository = transactionRepository;
        this.financeService = financeService;
    }

    public Map<String, Object> getMonthlyTrend(AppUser user) {
        List<Map<String, Object>> rows = new ArrayList<>();
        YearMonth end = YearMonth.now();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = end.minusMonths(i);
            List<Transaction> list = transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                user.getId(), ym.atDay(1), ym.atEndOfMonth()
            );
            rows.add(Map.of(
                "month", ym.getMonth().toString().substring(0, 3),
                "income", financeService.sumByType(list, "INCOME"),
                "expenses", financeService.sumByType(list, "EXPENSE")
            ));
        }
        return Map.of("months", rows);
    }

    public List<Map<String, Object>> getCategoryBreakdown(AppUser user) {
        Map<String, BigDecimal> grouped = transactionRepository.findByUserIdOrderByTransactionDateDesc(user.getId())
            .stream()
            .filter(t -> t.getType().equals("EXPENSE"))
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
            ));

        return grouped.entrySet().stream()
            .map(e -> Map.<String, Object>of("category", e.getKey(), "amount", e.getValue()))
            .toList();
    }

    public Map<String, Object> getExpenseForecast(AppUser user) {
        YearMonth end = YearMonth.now();
        List<BigDecimal> history = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            YearMonth ym = end.minusMonths(i);
            List<Transaction> list = transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                user.getId(), ym.atDay(1), ym.atEndOfMonth()
            );
            history.add(financeService.sumByType(list, "EXPENSE"));
        }

        BigDecimal avg = history.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

        return Map.of("method", "Three-month moving average", "forecast", avg, "history", history);
    }

    public List<Map<String, Object>> detectSpendingAnomalies(AppUser user) {
        List<Transaction> expenses = transactionRepository.findByUserIdOrderByTransactionDateDesc(user.getId())
            .stream()
            .filter(t -> t.getType().equals("EXPENSE"))
            .toList();

        Map<String, List<Transaction>> byCategory = expenses.stream()
            .collect(Collectors.groupingBy(Transaction::getCategory));

        List<Map<String, Object>> anomalies = new ArrayList<>();
        for (var entry : byCategory.entrySet()) {
            List<Transaction> values = entry.getValue();
            if (values.size() < 4) continue;

            double mean = values.stream().mapToDouble(t -> t.getAmount().doubleValue()).average().orElse(0);
            double variance = values.stream().mapToDouble(t -> Math.pow(t.getAmount().doubleValue() - mean, 2)).average().orElse(0);
            double stdDev = Math.sqrt(variance);
            if (stdDev == 0) continue;

            for (Transaction tx : values.stream().limit(30).toList()) {
                double zScore = (tx.getAmount().doubleValue() - mean) / stdDev;
                if (zScore > 2.0) {
                    anomalies.add(Map.of(
                        "id", tx.getId(),
                        "category", tx.getCategory(),
                        "amount", tx.getAmount(),
                        "date", tx.getTransactionDate().toString(),
                        "zScore", BigDecimal.valueOf(zScore).setScale(2, RoundingMode.HALF_UP)
                    ));
                }
            }
        }
        return anomalies;
    }

    public String buildMonthlyCsv(AppUser user, YearMonth ym) {
        StringBuilder sb = new StringBuilder("Date,Type,Category,Description,Amount\n");
        List<Transaction> list = transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            user.getId(), ym.atDay(1), ym.atEndOfMonth()
        );
        for (Transaction t : list) {
            String desc = t.getDescription() == null ? "" : t.getDescription().replace("\"", "\"\"");
            sb.append(t.getTransactionDate()).append(",")
              .append(t.getType()).append(",")
              .append(t.getCategory()).append(",\"")
              .append(desc).append("\",")
              .append(t.getAmount()).append("\n");
        }
        return sb.toString();
    }
}
