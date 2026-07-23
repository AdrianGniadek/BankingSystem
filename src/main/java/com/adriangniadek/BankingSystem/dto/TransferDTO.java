package com.adriangniadek.BankingSystem.dto;

import com.adriangniadek.BankingSystem.enums.TransferStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferDTO(Long id,
                          String sourceAccountNumber,
                          String targetAccountNumber,
                          BigDecimal amount,
                          String currency,
                          String description,
                          TransferStatus status,
                          LocalDateTime createdAt) {
}
