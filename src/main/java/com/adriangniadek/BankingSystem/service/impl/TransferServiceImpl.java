package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.CreateTransferRequest;
import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.enums.TransferStatus;
import com.adriangniadek.BankingSystem.exception.BusinessRuleViolationException;
import com.adriangniadek.BankingSystem.exception.ResourceNotFoundException;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.Transfer;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.TransferRepository;
import com.adriangniadek.BankingSystem.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @bankingAuthorization.canAccessAccount(#request.sourceAccountId(), authentication)")
    public TransferDTO createTransfer(CreateTransferRequest request) {
        validateRequest(request);

        Long firstAccountId = Math.min(request.sourceAccountId(), request.targetAccountId());
        Long secondAccountId = Math.max(request.sourceAccountId(), request.targetAccountId());

        Account firstAccount = findAccountForUpdate(firstAccountId, request);
        Account secondAccount = findAccountForUpdate(secondAccountId, request);
        Account sourceAccount = request.sourceAccountId().equals(firstAccountId) ? firstAccount : secondAccount;
        Account targetAccount = request.targetAccountId().equals(firstAccountId) ? firstAccount : secondAccount;

        validateAccounts(sourceAccount, targetAccount, request);

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.amount()));
        targetAccount.setBalance(targetAccount.getBalance().add(request.amount()));

        Transfer transfer = new Transfer();
        transfer.setSourceAccount(sourceAccount);
        transfer.setTargetAccount(targetAccount);
        transfer.setAmount(request.amount());
        transfer.setCurrency(request.currency());
        transfer.setDescription(request.description());
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setCreatedAt(LocalDateTime.now());

        return toDto(transferRepository.save(transfer));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @bankingAuthorization.canAccessAccount(#accountId, authentication)")
    public List<TransferDTO> getTransfersForAccount(Long accountId) {
        return transferRepository
                .findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(accountId, accountId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private void validateRequest(CreateTransferRequest request) {
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException("Transfer amount must be greater than zero");
        }
        if (request.sourceAccountId().equals(request.targetAccountId())) {
            throw new BusinessRuleViolationException("Source and target accounts cannot be the same");
        }
    }

    private Account findAccountForUpdate(Long accountId, CreateTransferRequest request) {
        String accountType = accountId.equals(request.sourceAccountId()) ? "Source" : "Target";
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(accountType + " account not found"));
    }

    private void validateAccounts(
            Account sourceAccount, Account targetAccount, CreateTransferRequest request) {
        if (!sourceAccount.getCurrency().equals(request.currency())
                || !targetAccount.getCurrency().equals(request.currency())) {
            throw new BusinessRuleViolationException(
                    "Transfer currency must match both account currencies");
        }
        if (sourceAccount.getBalance().compareTo(request.amount()) < 0) {
            throw new BusinessRuleViolationException("Insufficient funds in source account");
        }
    }

    private TransferDTO toDto(Transfer transfer) {
        return new TransferDTO(
                transfer.getId(),
                transfer.getSourceAccount().getId(),
                transfer.getTargetAccount().getId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getDescription(),
                transfer.getStatus(),
                transfer.getCreatedAt());
    }
}
