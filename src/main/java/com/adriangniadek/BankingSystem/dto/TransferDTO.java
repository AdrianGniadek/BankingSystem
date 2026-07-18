package com.adriangniadek.BankingSystem.dto;

import com.adriangniadek.BankingSystem.enums.TransferStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferDTO(Long id,

                          @NotNull(message = "Source account ID is required")
                          @Positive(message = "Source account ID must be positive")
                          Long sourceAccountId,

                          @NotNull(message = "Target account ID is required")
                          @Positive(message = "Target account ID must be positive")
                          Long targetAccountId,

                          @NotNull(message = "Amount is required")
                          @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be greater than 0")
                          BigDecimal amount,

                          @NotBlank(message = "Currency is required")
                          @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter uppercase ISO code")
                          String currency,

                          @Size(max = 255, message = "Description must not exceed 255 characters")
                          String description,

                          TransferStatus status,

                          LocalDateTime createdAt) {
}
