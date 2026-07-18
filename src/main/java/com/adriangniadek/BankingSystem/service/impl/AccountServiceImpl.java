package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.AccountStatementDTO;
import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.exception.BusinessRuleViolationException;
import com.adriangniadek.BankingSystem.exception.ResourceNotFoundException;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.Transfer;
import com.adriangniadek.BankingSystem.model.User;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.TransferRepository;
import com.adriangniadek.BankingSystem.repository.UserRepository;
import com.adriangniadek.BankingSystem.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransferRepository transferRepository;

    @Override
    @PreAuthorize("hasRole('ADMIN') or @bankingAuthorization.canAccessUser(#userId, authentication)")
    public AccountDTO createAccount(Long userId, AccountDTO accountDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
    @PreAuthorize("hasRole('ADMIN') or @bankingAuthorization.canAccessUser(#userId, authentication)")
    public List<AccountDTO> getUserAccounts(Long userId) {
        return accountRepository.findAll().stream()
                .filter(account -> account.getUser().getId().equals(userId))
                .map(account -> new AccountDTO(account.getId(), account.getAccountNumber(),
                        account.getAccountType(), account.getBalance(),
                        account.getCurrency(), account.getUser().getId()))
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @bankingAuthorization.canAccessAccount(#accountId, authentication)")
    public BigDecimal getAccountBalance(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return account.getBalance();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @bankingAuthorization.canAccessAccount(#accountId, authentication)")
    public AccountDTO getAccountById(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
        return new AccountDTO(
            account.getId(),
            account.getAccountNumber(),
            account.getAccountType(),
            account.getBalance(),
            account.getCurrency(),
            account.getUser().getId()
        );
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @bankingAuthorization.canAccessAccount(#accountId, authentication)")
    public AccountStatementDTO generateAccountStatement(Long accountId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessRuleViolationException("Start date must not be after end date");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        List<Transfer> transfers = transferRepository.findByAccountIdAndDateRange(accountId, startDate, endDate);

        BigDecimal openingBalance = account.getBalance();
        
        for (Transfer transfer : transfers) {
            if (transfer.getSourceAccount().getId().equals(accountId)) {
                openingBalance = openingBalance.add(transfer.getAmount());
            } else if (transfer.getTargetAccount().getId().equals(accountId)) {
                openingBalance = openingBalance.subtract(transfer.getAmount());
            }
        }

        List<TransferDTO> transferDTOs = transfers.stream()
                .map(t -> new TransferDTO(
                        t.getId(),
                        t.getSourceAccount().getId(),
                        t.getTargetAccount().getId(),
                        t.getAmount(),
                        t.getCurrency(),
                        t.getDescription(),
                        t.getStatus(),
                        t.getCreatedAt()))
                .toList();
        
        return new AccountStatementDTO(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getCurrency(),
                startDate,
                endDate,
                openingBalance,
                account.getBalance(),
                transferDTOs
        );
    }

}
