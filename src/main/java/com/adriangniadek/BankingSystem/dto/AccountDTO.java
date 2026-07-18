package com.adriangniadek.BankingSystem.dto;

import com.adriangniadek.BankingSystem.enums.AccountType;

import java.math.BigDecimal;

public record AccountDTO(Long id,
                         String accountNumber,
                         AccountType accountType,
                         BigDecimal balance,
                         String currency,
                         Long userId) {
}
