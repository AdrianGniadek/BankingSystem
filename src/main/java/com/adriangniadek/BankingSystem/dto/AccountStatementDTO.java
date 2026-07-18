package com.adriangniadek.BankingSystem.dto;

import com.adriangniadek.BankingSystem.enums.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AccountStatementDTO(
    Long accountId,
    String accountNumber,
    AccountType accountType,
    String currency,
    LocalDateTime startDate,
    LocalDateTime endDate,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    List<TransferDTO> transactions
) {
}
