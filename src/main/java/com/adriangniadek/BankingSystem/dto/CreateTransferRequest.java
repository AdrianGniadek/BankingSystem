package com.adriangniadek.BankingSystem.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateTransferRequest(
        @NotNull(message = "Source account ID is required")
        @Positive(message = "Source account ID must be positive")
        Long sourceAccountId,

        @NotNull(message = "Target account ID is required")
        @Positive(message = "Target account ID must be positive")
        Long targetAccountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter uppercase ISO code")
        String currency,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description) {
}
