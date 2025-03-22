package com.adriangniadek.BankingSystem.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AccountDTO(Long id,
                         @NotBlank(message = "Account number is required")
                         @Size(min = 10, max = 20, message = "Account number must be between 10 and 20 characters")
                         String accountNumber,

                         @NotBlank(message = "Account type is required")
                         @Size(max = 20, message = "Account type must not exceed 20 characters")
                         String accountType,

                         @NotNull(message = "Balance is required")
                         @DecimalMin(value = "0.0", inclusive = true, message = "Balance cannot be negative")
                         BigDecimal balance,

                         @NotBlank(message = "Currency is required")
                         @Size(min = 3, max = 3, message = "Currency must be 3-letter ISO code")
                         String currency,

                         @NotNull(message = "User ID is required")
                         Long userId) {
}
