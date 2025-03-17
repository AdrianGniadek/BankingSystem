package com.adriangniadek.BankingSystem.dto;

import java.math.BigDecimal;

public record AccountDTO(Long id,
                         String accountNumber,
                         String type,
                         BigDecimal balance,
                         String currency,
                         Long userId) {
}
