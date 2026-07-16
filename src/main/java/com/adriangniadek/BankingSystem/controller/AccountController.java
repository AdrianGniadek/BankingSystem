package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.AccountStatementDTO;
import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Validated
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/{userId}")
    public ResponseEntity<AccountDTO> createAccount(
            @PathVariable @Positive(message = "User ID must be positive") Long userId,
            @RequestBody @Valid AccountDTO accountDTO) {
        AccountDTO savedAccount = accountService.createAccount(userId, accountDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedAccount);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<AccountDTO>> getUserAccounts(
            @PathVariable @Positive(message = "User ID must be positive") Long userId) {
        List<AccountDTO> accounts = accountService.getUserAccounts(userId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/details/{accountId}")
    public ResponseEntity<AccountDTO> getAccountDetails(
            @PathVariable @Positive(message = "Account ID must be positive") Long accountId) {
        AccountDTO account = accountService.getAccountById(accountId);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/balance/{accountId}")
    public ResponseEntity<BigDecimal> getAccountBalance(
            @PathVariable @Positive(message = "Account ID must be positive") Long accountId) {
        BigDecimal balance = accountService.getAccountBalance(accountId);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/transactions/{accountId}")
    public ResponseEntity<List<TransferDTO>> getTransactionHistory(
            @PathVariable @Positive(message = "Account ID must be positive") Long accountId) {
        List<TransferDTO> history = accountService.getAccountTransactionHistory(accountId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/statement/{accountId}")
    public ResponseEntity<AccountStatementDTO> getAccountStatement(
            @PathVariable @Positive(message = "Account ID must be positive") Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        AccountStatementDTO statement = accountService.generateAccountStatement(accountId, startDate, endDate);
        return ResponseEntity.ok(statement);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferDTO> transferMoney(
            @RequestParam @Positive(message = "Source account ID must be positive") Long sourceAccountId,
            @RequestParam @Positive(message = "Target account ID must be positive") Long targetAccountId,
            @RequestParam @DecimalMin(value = "0.01", message = "Amount must be at least 0.01") BigDecimal amount,
            @RequestParam @Pattern(
                    regexp = "[A-Z]{3}",
                    message = "Currency must be a 3-letter uppercase ISO code") String currency,
            @RequestParam(required = false)
            @Size(max = 255, message = "Description must not exceed 255 characters") String description) {

        TransferDTO transfer = accountService.transferMoney(
                sourceAccountId, targetAccountId, amount, currency, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(transfer);
    }
}
