package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.AccountEntryDTO;
import com.adriangniadek.BankingSystem.dto.CreateDemoDepositRequest;
import com.adriangniadek.BankingSystem.dto.CreateDepositRequest;
import com.adriangniadek.BankingSystem.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo/accounts")
@RequiredArgsConstructor
@Validated
@ConditionalOnProperty(prefix = "features.demo-funding", name = "enabled", havingValue = "true")
public class DemoFundingController {

    private static final String DEMO_DEPOSIT_DESCRIPTION = "Demo account funding";

    private final AccountService accountService;

    @PostMapping("/{accountId}/deposits")
    public ResponseEntity<AccountEntryDTO> fundAccount(
            @PathVariable @Positive(message = "Account ID must be positive") Long accountId,
            @RequestBody @Valid CreateDemoDepositRequest request,
            Authentication authentication) {
        CreateDepositRequest deposit = new CreateDepositRequest(
                request.idempotencyKey(),
                request.amount(),
                request.currency(),
                DEMO_DEPOSIT_DESCRIPTION);
        AccountEntryDTO entry = accountService.depositCurrentUserAccount(
                accountId, deposit, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }
}
