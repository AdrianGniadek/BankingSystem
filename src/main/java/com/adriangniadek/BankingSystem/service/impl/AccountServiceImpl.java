package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.AccountEntryDTO;
import com.adriangniadek.BankingSystem.dto.AccountStatementDTO;
import com.adriangniadek.BankingSystem.dto.CreateAccountRequest;
import com.adriangniadek.BankingSystem.dto.CreateDepositRequest;
import com.adriangniadek.BankingSystem.dto.UpdateAccountStatusRequest;
import com.adriangniadek.BankingSystem.enums.AccountEntryType;
import com.adriangniadek.BankingSystem.enums.AccountStatus;
import com.adriangniadek.BankingSystem.exception.BusinessRuleViolationException;
import com.adriangniadek.BankingSystem.exception.ResourceConflictException;
import com.adriangniadek.BankingSystem.exception.ResourceNotFoundException;
import com.adriangniadek.BankingSystem.mapper.AccountEntryMapper;
import com.adriangniadek.BankingSystem.mapper.AccountMapper;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.AccountEntry;
import com.adriangniadek.BankingSystem.model.User;
import com.adriangniadek.BankingSystem.repository.AccountEntryRepository;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
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
    private final AccountEntryRepository accountEntryRepository;
    private final AccountMapper accountMapper;
    private final AccountEntryMapper accountEntryMapper;
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
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AccountEntryDTO deposit(Long accountId, CreateDepositRequest request, String createdBy) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        String idempotencyKey = request.idempotencyKey().toString();
        AccountEntry existingEntry = accountEntryRepository.findByIdempotencyKey(idempotencyKey)
                .orElse(null);
        if (existingEntry != null) {
            validateRepeatedDeposit(existingEntry, accountId, request);
            return accountEntryMapper.toDto(existingEntry);
        }

        requireActiveAccount(account, "Deposits require an active account");

        if (!account.getCurrency().equals(request.currency())) {
            throw new BusinessRuleViolationException("Deposit currency must match account currency");
        }

        account.setBalance(account.getBalance().add(request.amount()));

        AccountEntry entry = new AccountEntry();
        entry.setAccount(account);
        entry.setType(AccountEntryType.DEPOSIT);
        entry.setAmount(request.amount());
        entry.setCurrency(request.currency());
        entry.setDescription(request.description());
        entry.setCreatedAt(LocalDateTime.now());
        entry.setCreatedBy(createdBy);
        entry.setIdempotencyKey(idempotencyKey);

        return accountEntryMapper.toDto(accountEntryRepository.save(entry));
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

        List<AccountEntry> entriesFromStart = accountEntryRepository.findByAccountIdFromDate(
                accountId, startDate);

        BigDecimal openingBalance = reverseEntries(account.getBalance(), entriesFromStart);
        BigDecimal closingBalance = reverseEntries(
                account.getBalance(),
                entriesFromStart.stream()
                        .filter(entry -> entry.getCreatedAt().isAfter(endDate))
                        .toList());

        List<AccountEntryDTO> entryDTOs = entriesFromStart.stream()
                .filter(entry -> !entry.getCreatedAt().isAfter(endDate))
                .map(accountEntryMapper::toDto)
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
                entryDTOs
        );
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AccountDTO updateAccountStatus(Long accountId, UpdateAccountStatusRequest request) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (request.status() == AccountStatus.CLOSED) {
            throw new BusinessRuleViolationException(
                    "Closed status can only be set by closing the account");
        }
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BusinessRuleViolationException("Closed account status cannot be changed");
        }

        account.setStatus(request.status());
        return accountMapper.toDto(account);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @bankingAuthorization.canAccessAccount(#accountId, authentication)")
    public void closeAccount(Long accountId) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (account.getStatus() == AccountStatus.CLOSED) {
            return;
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessRuleViolationException(
                    "Account balance must be zero before closing");
        }

        account.setStatus(AccountStatus.CLOSED);
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
        account.setStatus(AccountStatus.ACTIVE);
        account.setUser(user);

        return accountMapper.toDto(accountRepository.save(account));
    }

    private BigDecimal reverseEntries(BigDecimal balance, List<AccountEntry> entries) {
        BigDecimal historicalBalance = balance;
        for (AccountEntry entry : entries) {
            historicalBalance = switch (entry.getType()) {
                case TRANSFER_OUT -> historicalBalance.add(entry.getAmount());
                case DEPOSIT, TRANSFER_IN -> historicalBalance.subtract(entry.getAmount());
            };
        }
        return historicalBalance;
    }

    private void validateRepeatedDeposit(
            AccountEntry entry, Long accountId, CreateDepositRequest request) {
        boolean sameRequest = entry.getType() == AccountEntryType.DEPOSIT
                && entry.getAccount().getId().equals(accountId)
                && entry.getAmount().compareTo(request.amount()) == 0
                && entry.getCurrency().equals(request.currency())
                && entry.getDescription().equals(request.description());
        if (!sameRequest) {
            throw new ResourceConflictException("Idempotency key was already used for another operation");
        }
    }

    private void requireActiveAccount(Account account, String message) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleViolationException(message);
        }
    }

}
