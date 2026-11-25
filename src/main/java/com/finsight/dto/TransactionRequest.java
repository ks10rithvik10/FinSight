package com.finsight.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
    @DecimalMin("0.01") BigDecimal amount,
    @Pattern(regexp = "INCOME|EXPENSE") String type,
    @NotBlank String category,
    String description,
    LocalDate transactionDate
) {}
