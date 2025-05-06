package com.adriangniadek.BankingSystem.service;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.TransferDTO;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    AccountDTO createAccount(Long userId, AccountDTO accountDTO);
    List<AccountDTO> getUserAccounts(Long userId);
    BigDecimal getAccountBalance(Long accountId);

    List<TransferDTO> getAccountTransactionHistory(Long accountId);

    @Transactional
    TransferDTO transferMoney(Long sourceAccountId, Long targetAccountId, BigDecimal amount,
                              String currency, String description);
}
