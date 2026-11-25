package com.finsight.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record BudgetRequest(
    @NotBlank String category,
    @Min(1) @Max(12) int month,
    @Min(2020) int year,
    @DecimalMin("1.00") BigDecimal limitAmount
) {}
