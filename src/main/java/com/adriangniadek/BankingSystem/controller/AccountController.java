package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.AccountEntryDTO;
import com.adriangniadek.BankingSystem.dto.AccountStatementDTO;
import com.adriangniadek.BankingSystem.dto.CreateAccountRequest;
import com.adriangniadek.BankingSystem.dto.CreateDepositRequest;
import com.adriangniadek.BankingSystem.dto.UpdateAccountStatusRequest;
import com.adriangniadek.BankingSystem.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    @PostMapping("/me")
    public ResponseEntity<AccountDTO> createCurrentUserAccount(
            @RequestBody @Valid CreateAccountRequest request,
            Authentication authentication) {
        AccountDTO savedAccount = accountService.createCurrentUserAccount(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedAccount);
    }

    @GetMapping("/me")
    public ResponseEntity<List<AccountDTO>> getCurrentUserAccounts(Authentication authentication) {
        return ResponseEntity.ok(accountService.getCurrentUserAccounts(authentication.getName()));
    }

    @PostMapping("/{accountId}/deposits")
    public ResponseEntity<AccountEntryDTO> deposit(
            @PathVariable @Positive(message = "Account ID must be positive") Long accountId,
            @RequestBody @Valid CreateDepositRequest request,
            Authentication authentication) {
        AccountEntryDTO entry = accountService.deposit(accountId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    @PostMapping("/{userId}")
    public ResponseEntity<AccountDTO> createAccount(
            @PathVariable @Positive(message = "User ID must be positive") Long userId,
            @RequestBody @Valid CreateAccountRequest request) {
        AccountDTO savedAccount = accountService.createAccount(userId, request);
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

    @GetMapping("/statement/{accountId}")
    public ResponseEntity<AccountStatementDTO> getAccountStatement(
            @PathVariable @Positive(message = "Account ID must be positive") Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        AccountStatementDTO statement = accountService.generateAccountStatement(accountId, startDate, endDate);
        return ResponseEntity.ok(statement);
    }

    @PatchMapping("/{accountId}/status")
    public ResponseEntity<AccountDTO> updateAccountStatus(
            @PathVariable @Positive(message = "Account ID must be positive") Long accountId,
            @RequestBody @Valid UpdateAccountStatusRequest request) {
        return ResponseEntity.ok(accountService.updateAccountStatus(accountId, request));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> closeAccount(
            @PathVariable @Positive(message = "Account ID must be positive") Long accountId) {
        accountService.closeAccount(accountId);
        return ResponseEntity.noContent().build();
    }

}
