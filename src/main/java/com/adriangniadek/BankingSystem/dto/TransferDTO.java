package com.adriangniadek.BankingSystem.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferDTO(Long id,

                          @NotNull(message = "Source account ID is required")
                          Long sourceAccountId,

                          @NotNull(message = "Target account ID is required")
                          Long targetAccountId,

                          @NotNull(message = "Amount is required")
                          @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be greater than 0")
                          BigDecimal amount,

                          @NotBlank(message = "Currency is required")
                          @Size(min = 3, max = 3, message = "Currency must be 3-letter ISO code")
                          String currency,

                          @Size(max = 255, message = "Description must not exceed 255 characters")
                          String description,

                          @NotBlank(message = "Status is required")
                          @Size(max = 20, message = "Status must not exceed 20 characters")
                          String status,

                          LocalDateTime createdAt) {
}
