package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.CreateTransferRequest;
import com.adriangniadek.BankingSystem.dto.PageResponse;
import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.service.TransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
@Validated
public class TransferController {
    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferDTO> createTransfer(
            @RequestBody @Valid CreateTransferRequest request,
            Authentication authentication) {
        TransferDTO savedTransfer = transferService.createTransfer(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTransfer);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<PageResponse<TransferDTO>> getTransfersForAccount(
            @PathVariable @Positive(message = "Account ID must be positive") Long accountId,
            @RequestParam(defaultValue = "0")
            @PositiveOrZero(message = "Page must not be negative") int page,
            @RequestParam(defaultValue = "20")
            @Positive(message = "Page size must be positive")
            @Max(value = 100, message = "Page size must not exceed 100") int size) {
        PageResponse<TransferDTO> transfers = transferService.getTransfersForAccount(accountId, page, size);
        return ResponseEntity.ok(transfers);
    }
}
