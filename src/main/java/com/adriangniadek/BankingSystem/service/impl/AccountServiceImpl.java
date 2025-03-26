package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.User;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.TransferRepository;
import com.adriangniadek.BankingSystem.repository.UserRepository;
import com.adriangniadek.BankingSystem.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private TransferRepository transferRepository;

    @Override
    public AccountDTO createAccount(Long userId, AccountDTO accountDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Account account = new Account();
        account.setAccountNumber(accountDTO.accountNumber());
        account.setAccountType(accountDTO.accountType());
        account.setBalance(accountDTO.balance());
        account.setCurrency(accountDTO.currency());
        account.setUser(user);

        Account savedAccount = accountRepository.save(account);
        return new AccountDTO(savedAccount.getId(), savedAccount.getAccountNumber(),
                savedAccount.getAccountType(), savedAccount.getBalance(),
                savedAccount.getCurrency(), savedAccount.getUser().getId());
    }

    @Override
    public List<AccountDTO> getUserAccounts(Long userId) {
        return accountRepository.findAll().stream()
                .filter(account -> account.getUser().getId().equals(userId))
                .map(account -> new AccountDTO(account.getId(), account.getAccountNumber(),
                        account.getAccountType(), account.getBalance(),
                        account.getCurrency(), account.getUser().getId()))
                .toList();
    }

    @Override
    public BigDecimal getAccountBalance(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return account.getBalance();
    }

    @Override
    public List<TransferDTO> getAccountTransactionHistory(Long accountId) {
        return transferRepository.findBySourceAccountId(accountId).stream()
                .map(t -> new TransferDTO(t.getId(), t.getSourceAccount().getId(),
                        t.getTargetAccount().getId(), t.getAmount(),
                        t.getCurrency(), t.getDescription(), t.getStatus(), t.getCreatedAt()))
                .toList();
    }
}