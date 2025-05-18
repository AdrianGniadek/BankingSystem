package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.AccountStatementDTO;
import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.Transfer;
import com.adriangniadek.BankingSystem.model.User;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.TransferRepository;
import com.adriangniadek.BankingSystem.repository.UserRepository;
import com.adriangniadek.BankingSystem.service.AccountService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
    public AccountDTO getAccountById(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
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
    public AccountStatementDTO generateAccountStatement(Long accountId, LocalDateTime startDate, LocalDateTime endDate) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

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

    @Override
    public List<TransferDTO> getAccountTransactionHistory(Long accountId) {
        return transferRepository.findBySourceAccountId(accountId).stream()
                .map(t -> new TransferDTO(t.getId(), t.getSourceAccount().getId(),
                        t.getTargetAccount().getId(), t.getAmount(),
                        t.getCurrency(), t.getDescription(), t.getStatus(), t.getCreatedAt()))
                .toList();
    }
    @Transactional
    @Override
    public TransferDTO transferMoney(Long sourceAccountId, Long targetAccountId, BigDecimal amount,
                                     String currency, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        if (sourceAccountId.equals(targetAccountId)) {
            throw new IllegalArgumentException("Source and target accounts cannot be the same");
        }

        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        Account targetAccount = accountRepository.findById(targetAccountId)
                .orElseThrow(() -> new RuntimeException("Target account not found"));

        if (!sourceAccount.getCurrency().equals(currency) || !targetAccount.getCurrency().equals(currency)) {
            throw new IllegalArgumentException("Currency mismatch. Transfer currency must match both account currencies");
        }

        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds in source account");
        }

        Transfer transfer = new Transfer();
        transfer.setSourceAccount(sourceAccount);
        transfer.setTargetAccount(targetAccount);
        transfer.setAmount(amount);
        transfer.setCurrency(currency);
        transfer.setDescription(description);
        transfer.setStatus("COMPLETED");
        transfer.setCreatedAt(LocalDateTime.now());

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        targetAccount.setBalance(targetAccount.getBalance().add(amount));

        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);
        Transfer savedTransfer = transferRepository.save(transfer);

        return new TransferDTO(
                savedTransfer.getId(),
                savedTransfer.getSourceAccount().getId(),
                savedTransfer.getTargetAccount().getId(),
                savedTransfer.getAmount(),
                savedTransfer.getCurrency(),
                savedTransfer.getDescription(),
                savedTransfer.getStatus(),
                savedTransfer.getCreatedAt()
        );
    }
}