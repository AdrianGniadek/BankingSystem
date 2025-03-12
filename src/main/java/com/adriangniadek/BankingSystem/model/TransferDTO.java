package com.adriangniadek.BankingSystem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferDTO(Long id,
                          Long sourceAccountId,
                          Long targetAccountId,
                          BigDecimal amount,
                          String currency,
                          String description,
                          String status,
                          LocalDateTime createdAt) {
}
