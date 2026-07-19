package com.adriangniadek.BankingSystem.service;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.AccountStatementDTO;
import com.adriangniadek.BankingSystem.dto.CreateAccountRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AccountService {
    AccountDTO createCurrentUserAccount(String email, CreateAccountRequest request);
    List<AccountDTO> getCurrentUserAccounts(String email);
    AccountDTO createAccount(Long userId, CreateAccountRequest request);
    List<AccountDTO> getUserAccounts(Long userId);
    BigDecimal getAccountBalance(Long accountId);
    AccountDTO getAccountById(Long accountId);
    AccountStatementDTO generateAccountStatement(Long accountId, LocalDateTime startDate, LocalDateTime endDate);
}
