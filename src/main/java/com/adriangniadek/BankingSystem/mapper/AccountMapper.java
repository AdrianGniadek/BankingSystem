package com.adriangniadek.BankingSystem.mapper;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.model.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountDTO toDto(Account account) {
        return new AccountDTO(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency(),
                account.getUser().getId()
        );
    }

    public Account toEntity(AccountDTO accountDTO) {
        Account account = new Account();
        account.setId(accountDTO.id());
        account.setAccountNumber(accountDTO.accountNumber());
        account.setAccountType(accountDTO.accountType());
        account.setBalance(accountDTO.balance());
        account.setCurrency(accountDTO.currency());

        return account;
    }
}

