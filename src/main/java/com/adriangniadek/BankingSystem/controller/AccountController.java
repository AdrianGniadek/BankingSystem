package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/{userId}")
    public ResponseEntity<AccountDTO> createAccount(@PathVariable Long userId, @RequestBody @Valid AccountDTO accountDTO) {
        AccountDTO savedAccount = accountService.createAccount(userId, accountDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedAccount);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<AccountDTO>> getUserAccounts(@PathVariable Long userId) {
        List<AccountDTO> accounts = accountService.getUserAccounts(userId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/balance/{accountId}")
    public ResponseEntity<BigDecimal> getAccountBalance(@PathVariable Long accountId) {
        BigDecimal balance = accountService.getAccountBalance(accountId);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/transactions/{accountId}")
    public ResponseEntity<List<TransferDTO>> getTransactionHistory(@PathVariable Long accountId) {
        List<TransferDTO> history = accountService.getAccountTransactionHistory(accountId);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferDTO> transferMoney(
            @RequestParam Long sourceAccountId,
            @RequestParam Long targetAccountId,
            @RequestParam BigDecimal amount,
            @RequestParam String currency,
            @RequestParam(required = false) String description) {

        TransferDTO transfer = accountService.transferMoney(
                sourceAccountId, targetAccountId, amount, currency, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(transfer);
    }
}