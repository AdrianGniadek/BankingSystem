package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.Transfer;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransferServiceImplTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransferServiceImpl transferService;

    private Account sourceAccount;
    private Account targetAccount;
    private Transfer transfer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        sourceAccount = new Account();
        sourceAccount.setId(1L);
        sourceAccount.setBalance(new BigDecimal("500.00"));

        targetAccount = new Account();
        targetAccount.setId(2L);
        targetAccount.setBalance(new BigDecimal("300.00"));

        transfer = new Transfer(1L, sourceAccount, targetAccount, new BigDecimal("100.00"), "USD", "Test transfer", "PENDING", LocalDateTime.now());
    }

    @Test
    void shouldCreateTransferSuccessfully() {
        TransferDTO transferDTO = new TransferDTO(null, 1L, 2L, new BigDecimal("100.00"), "USD", "Test transfer", "PENDING", LocalDateTime.now());
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(targetAccount));
        when(transferRepository.save(any(Transfer.class))).thenReturn(transfer);

        TransferDTO result = transferService.createTransfer(transferDTO);

        assertNotNull(result);
        assertEquals(new BigDecimal("100.00"), result.amount());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transferRepository, times(1)).save(any(Transfer.class));
    }

    @Test
    void shouldThrowExceptionIfInsufficientFunds() {
        TransferDTO transferDTO = new TransferDTO(null, 1L, 2L, new BigDecimal("600.00"), "USD", "Test transfer", "PENDING", LocalDateTime.now());
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(targetAccount));

        assertThrows(RuntimeException.class, () -> transferService.createTransfer(transferDTO));
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    void shouldReturnTransfersForAccount() {
        when(transferRepository.findBySourceAccountId(1L)).thenReturn(List.of(transfer));

        List<TransferDTO> transfers = transferService.getTransfersForAccount(1L);

        assertEquals(1, transfers.size());
        assertEquals(new BigDecimal("100.00"), transfers.getFirst().amount());
    }
}
