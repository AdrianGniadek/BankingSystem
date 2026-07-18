package com.adriangniadek.BankingSystem.service;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.AccountStatementDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AccountService {
    AccountDTO createAccount(Long userId, AccountDTO accountDTO);
    List<AccountDTO> getUserAccounts(Long userId);
    BigDecimal getAccountBalance(Long accountId);
    AccountDTO getAccountById(Long accountId);
    AccountStatementDTO generateAccountStatement(Long accountId, LocalDateTime startDate, LocalDateTime endDate);
}
