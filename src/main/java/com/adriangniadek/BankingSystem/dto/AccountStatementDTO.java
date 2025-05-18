package com.adriangniadek.BankingSystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AccountStatementDTO(
    Long accountId,
    String accountNumber,
    String accountType,
    String currency,
    LocalDateTime startDate,
    LocalDateTime endDate,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    List<TransferDTO> transactions
) {
}
