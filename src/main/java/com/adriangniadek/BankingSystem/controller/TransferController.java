package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.service.TransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
@Validated
public class TransferController {
    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferDTO> createTransfer(@RequestBody @Valid TransferDTO transferDTO) {
        TransferDTO savedTransfer = transferService.createTransfer(transferDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTransfer);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<List<TransferDTO>> getTransfersForAccount(
            @PathVariable @Positive(message = "Account ID must be positive") Long accountId) {
        List<TransferDTO> transfers = transferService.getTransfersForAccount(accountId);
        return ResponseEntity.ok(transfers);
    }
}
