package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.CreateTransferRequest;
import com.adriangniadek.BankingSystem.dto.PageResponse;
import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.enums.AccountEntryType;
import com.adriangniadek.BankingSystem.enums.TransferStatus;
import com.adriangniadek.BankingSystem.exception.BusinessRuleViolationException;
import com.adriangniadek.BankingSystem.exception.ResourceConflictException;
import com.adriangniadek.BankingSystem.exception.ResourceNotFoundException;
import com.adriangniadek.BankingSystem.mapper.TransferMapper;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.AccountEntry;
import com.adriangniadek.BankingSystem.model.Transfer;
import com.adriangniadek.BankingSystem.repository.AccountEntryRepository;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.TransferRepository;
import com.adriangniadek.BankingSystem.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final AccountEntryRepository accountEntryRepository;
    private final TransferMapper transferMapper;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @bankingAuthorization.canAccessAccount(#request.sourceAccountId(), authentication)")
    public TransferDTO createTransfer(CreateTransferRequest request, String createdBy) {
        validateRequest(request);

        String idempotencyKey = request.idempotencyKey().toString();
        Long firstAccountId = Math.min(request.sourceAccountId(), request.targetAccountId());
        Long secondAccountId = Math.max(request.sourceAccountId(), request.targetAccountId());

        Account firstAccount = findAccountForUpdate(firstAccountId, request);
        Account secondAccount = findAccountForUpdate(secondAccountId, request);
        Account sourceAccount = request.sourceAccountId().equals(firstAccountId) ? firstAccount : secondAccount;
        Account targetAccount = request.targetAccountId().equals(firstAccountId) ? firstAccount : secondAccount;

        Transfer existingTransfer = transferRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existingTransfer != null) {
            validateRepeatedTransfer(existingTransfer, request);
            return transferMapper.toDto(existingTransfer);
        }

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
        transfer.setIdempotencyKey(idempotencyKey);

        Transfer savedTransfer = transferRepository.save(transfer);
        accountEntryRepository.saveAll(java.util.List.of(
                accountEntry(
                        sourceAccount, savedTransfer, AccountEntryType.TRANSFER_OUT,
                        request, savedTransfer.getCreatedAt(), createdBy),
                accountEntry(
                        targetAccount, savedTransfer, AccountEntryType.TRANSFER_IN,
                        request, savedTransfer.getCreatedAt(), createdBy)
        ));

        return transferMapper.toDto(savedTransfer);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or @bankingAuthorization.canAccessAccount(#accountId, authentication)")
    public PageResponse<TransferDTO> getTransfersForAccount(Long accountId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TransferDTO> transfers = transferRepository
                .findBySourceAccountIdOrTargetAccountId(accountId, accountId, pageRequest)
                .map(transferMapper::toDto);
        return PageResponse.from(transfers);
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

    private AccountEntry accountEntry(
            Account account,
            Transfer transfer,
            AccountEntryType type,
            CreateTransferRequest request,
            LocalDateTime createdAt,
            String createdBy) {
        AccountEntry entry = new AccountEntry();
        entry.setAccount(account);
        entry.setTransfer(transfer);
        entry.setType(type);
        entry.setAmount(request.amount());
        entry.setCurrency(request.currency());
        entry.setDescription(request.description());
        entry.setCreatedAt(createdAt);
        entry.setCreatedBy(createdBy);
        return entry;
    }

    private void validateRepeatedTransfer(Transfer transfer, CreateTransferRequest request) {
        boolean sameRequest = transfer.getSourceAccount().getId().equals(request.sourceAccountId())
                && transfer.getTargetAccount().getId().equals(request.targetAccountId())
                && transfer.getAmount().compareTo(request.amount()) == 0
                && transfer.getCurrency().equals(request.currency())
                && Objects.equals(transfer.getDescription(), request.description());
        if (!sameRequest) {
            throw new ResourceConflictException("Idempotency key was already used for another transfer");
        }
    }
}
