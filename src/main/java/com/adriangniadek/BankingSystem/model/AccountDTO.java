package com.adriangniadek.BankingSystem.model;

import java.math.BigDecimal;

public record AccountDTO(Long id,
                         String accountNumber,
                         String type,
                         BigDecimal balance,
                         String currency,
                         Long userId) {
}
