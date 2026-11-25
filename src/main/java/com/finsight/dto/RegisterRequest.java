package com.finsight.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Full name is required") String name,
    @Email(message = "Enter a valid email address") @NotBlank(message = "Email is required") String email,
    @Size(min = 8, message = "Password must be at least 8 characters") String password,
    @Size(max = 25, message = "Phone number is too long") String phone,
    @Pattern(regexp = "INR|USD|EUR|GBP", message = "Currency must be INR, USD, EUR, or GBP") String currency
) {}
