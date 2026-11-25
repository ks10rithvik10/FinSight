package com.finsight.controller;

import com.finsight.service.AnalyticsService;
import com.finsight.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final AuthService authService;
    private final AnalyticsService analyticsService;

    public AnalyticsController(AuthService authService, AnalyticsService analyticsService) {
        this.authService = authService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics/monthly")
    public Map<String, Object> monthlyTrend(Authentication auth) {
        return analyticsService.getMonthlyTrend(authService.getCurrentUser(auth));
    }

    @GetMapping("/analytics/categories")
    public List<Map<String, Object>> categoryBreakdown(Authentication auth) {
        return analyticsService.getCategoryBreakdown(authService.getCurrentUser(auth));
    }

    @GetMapping("/insights/forecast")
    public Map<String, Object> forecast(Authentication auth) {
        return analyticsService.getExpenseForecast(authService.getCurrentUser(auth));
    }

    @GetMapping("/insights/anomalies")
    public List<Map<String, Object>> anomalies(Authentication auth) {
        return analyticsService.detectSpendingAnomalies(authService.getCurrentUser(auth));
    }

    @GetMapping(value = "/reports/monthly", produces = "text/csv")
    public ResponseEntity<String> monthlyCsv(@RequestParam(defaultValue = "") String month, Authentication auth) {
        YearMonth ym;
        try {
            ym = month.isBlank() ? YearMonth.now() : YearMonth.parse(month);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use YYYY-MM format");
        }
        String csv = analyticsService.buildMonthlyCsv(authService.getCurrentUser(auth), ym);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=finsight-" + ym + ".csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv);
    }
}
