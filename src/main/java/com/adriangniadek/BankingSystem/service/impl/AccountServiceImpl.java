package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.AccountStatementDTO;
import com.adriangniadek.BankingSystem.dto.CreateAccountRequest;
import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.enums.TransferStatus;
import com.adriangniadek.BankingSystem.exception.BusinessRuleViolationException;
import com.adriangniadek.BankingSystem.exception.ResourceNotFoundException;
import com.adriangniadek.BankingSystem.mapper.AccountMapper;
import com.adriangniadek.BankingSystem.mapper.TransferMapper;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.Transfer;
import com.adriangniadek.BankingSystem.model.User;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.TransferRepository;
import com.adriangniadek.BankingSystem.repository.UserRepository;
import com.adriangniadek.BankingSystem.service.AccountService;
import com.adriangniadek.BankingSystem.service.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransferRepository transferRepository;
    private final AccountMapper accountMapper;
    private final TransferMapper transferMapper;
    private final AccountNumberGenerator accountNumberGenerator;

    @Override
    @Transactional
    @PreAuthorize("authentication.name == #email")
    public AccountDTO createCurrentUserAccount(String email, CreateAccountRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return createAccount(user, request);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("authentication.name == #email")
    public List<AccountDTO> getCurrentUserAccounts(String email) {
        return accountRepository.findByUserEmail(email).stream()
                .map(accountMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @bankingAuthorization.canAccessUser(#userId, authentication)")
    public AccountDTO createAccount(Long userId, CreateAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return createAccount(user, request);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @bankingAuthorization.canAccessUser(#userId, authentication)")
    public List<AccountDTO> getUserAccounts(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(accountMapper::toDto)
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
        
        return accountMapper.toDto(account);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or @bankingAuthorization.canAccessAccount(#accountId, authentication)")
    public AccountStatementDTO generateAccountStatement(Long accountId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessRuleViolationException("Start date must not be after end date");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        List<Transfer> transfersFromStart = transferRepository.findByAccountIdFromDate(
                accountId, startDate, TransferStatus.COMPLETED);

        BigDecimal openingBalance = reverseTransfers(account.getBalance(), transfersFromStart, accountId);
        BigDecimal closingBalance = reverseTransfers(
                account.getBalance(),
                transfersFromStart.stream()
                        .filter(transfer -> transfer.getCreatedAt().isAfter(endDate))
                        .toList(),
                accountId);

        List<TransferDTO> transferDTOs = transfersFromStart.stream()
                .filter(transfer -> !transfer.getCreatedAt().isAfter(endDate))
                .map(transferMapper::toDto)
                .toList();
        
        return new AccountStatementDTO(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getCurrency(),
                startDate,
                endDate,
                openingBalance,
                closingBalance,
                transferDTOs
        );
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            accountNumber = accountNumberGenerator.generate();
        } while (accountRepository.findByAccountNumber(accountNumber).isPresent());
        return accountNumber;
    }

    private AccountDTO createAccount(User user, CreateAccountRequest request) {
        Account account = new Account();
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setAccountType(request.accountType());
        account.setBalance(BigDecimal.ZERO.setScale(2));
        account.setCurrency(request.currency());
        account.setUser(user);

        return accountMapper.toDto(accountRepository.save(account));
    }

    private BigDecimal reverseTransfers(BigDecimal balance, List<Transfer> transfers, Long accountId) {
        BigDecimal historicalBalance = balance;
        for (Transfer transfer : transfers) {
            if (transfer.getSourceAccount().getId().equals(accountId)) {
                historicalBalance = historicalBalance.add(transfer.getAmount());
            } else {
                historicalBalance = historicalBalance.subtract(transfer.getAmount());
            }
        }
        return historicalBalance;
    }

}
