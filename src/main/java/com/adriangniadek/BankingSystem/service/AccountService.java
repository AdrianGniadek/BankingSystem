package com.adriangniadek.BankingSystem.service;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.AccountStatementDTO;
import com.adriangniadek.BankingSystem.dto.TransferDTO;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AccountService {
    AccountDTO createAccount(Long userId, AccountDTO accountDTO);
    List<AccountDTO> getUserAccounts(Long userId);
    BigDecimal getAccountBalance(Long accountId);
    AccountDTO getAccountById(Long accountId);
    List<TransferDTO> getAccountTransactionHistory(Long accountId);
    AccountStatementDTO generateAccountStatement(Long accountId, LocalDateTime startDate, LocalDateTime endDate);

    @Transactional
    TransferDTO transferMoney(Long sourceAccountId, Long targetAccountId, BigDecimal amount,
                              String currency, String description);
}
