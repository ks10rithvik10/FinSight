package com.finsight.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateSettingsRequest(
    @NotBlank(message = "Full name is required") String name,
    @Size(max = 25, message = "Phone number is too long") String phone,
    @Pattern(regexp = "INR|USD|EUR|GBP", message = "Currency must be INR, USD, EUR, or GBP") String currency,
    @Min(value = 0, message = "Savings target must be between 0 and 90")
    @Max(value = 90, message = "Savings target must be between 0 and 90") int savingsTargetPercent
) {}
