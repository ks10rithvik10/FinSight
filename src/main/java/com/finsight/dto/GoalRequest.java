package com.finsight.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalRequest(
    @NotBlank String name,
    @DecimalMin("0.01") BigDecimal targetAmount,
    @DecimalMin("0.00") BigDecimal currentAmount,
    LocalDate targetDate
) {}
