package com.adriangniadek.BankingSystem.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateDepositRequest(
        @NotNull(message = "Idempotency key is required")
        UUID idempotencyKey,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter uppercase ISO code")
        String currency,

        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) {
}
