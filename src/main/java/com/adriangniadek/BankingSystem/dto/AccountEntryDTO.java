package com.adriangniadek.BankingSystem.dto;

import com.adriangniadek.BankingSystem.enums.AccountEntryType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountEntryDTO(
        Long id,
        AccountEntryType type,
        BigDecimal amount,
        String currency,
        String description,
        LocalDateTime createdAt,
        Long transferId
) {
}
