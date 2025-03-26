package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.Transfer;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.TransferRepository;
import com.adriangniadek.BankingSystem.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;

    @Override
    public TransferDTO createTransfer(TransferDTO transferDTO) {
        Account source = accountRepository.findById(transferDTO.sourceAccountId())
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        Account target = accountRepository.findById(transferDTO.targetAccountId())
                .orElseThrow(() -> new RuntimeException("Target account not found"));

        if (source.getBalance().compareTo(transferDTO.amount()) < 0) {
            throw new RuntimeException("Insufficient funds in the source account");
        }

        source.setBalance(source.getBalance().subtract(transferDTO.amount()));
        accountRepository.save(source);
        target.setBalance(target.getBalance().add(transferDTO.amount()));
        accountRepository.save(target);

        Transfer transfer = new Transfer(null, source, target, transferDTO.amount(),
                transferDTO.currency(), transferDTO.description(), "PENDING",
                transferDTO.createdAt());

        Transfer savedTransfer = transferRepository.save(transfer);
        return new TransferDTO(savedTransfer.getId(), source.getId(), target.getId(),
                savedTransfer.getAmount(), savedTransfer.getCurrency(),
                savedTransfer.getDescription(), savedTransfer.getStatus(), savedTransfer.getCreatedAt());
    }

    @Override
    public List<TransferDTO> getTransfersForAccount(Long accountId) {
        return transferRepository.findBySourceAccountId(accountId).stream()
                .map(t -> new TransferDTO(t.getId(), t.getSourceAccount().getId(),
                        t.getTargetAccount().getId(), t.getAmount(),
                        t.getCurrency(), t.getDescription(), t.getStatus(), t.getCreatedAt()))
                .toList();
    }
}
