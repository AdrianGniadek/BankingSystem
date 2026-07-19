package com.adriangniadek.BankingSystem.mapper;

import com.adriangniadek.BankingSystem.dto.AccountEntryDTO;
import com.adriangniadek.BankingSystem.model.AccountEntry;
import org.springframework.stereotype.Component;

@Component
public class AccountEntryMapper {

    public AccountEntryDTO toDto(AccountEntry entry) {
        return new AccountEntryDTO(
                entry.getId(),
                entry.getType(),
                entry.getAmount(),
                entry.getCurrency(),
                entry.getDescription(),
                entry.getCreatedAt(),
                entry.getTransfer() == null ? null : entry.getTransfer().getId()
        );
    }
}
