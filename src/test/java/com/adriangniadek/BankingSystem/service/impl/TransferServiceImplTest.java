package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.CreateTransferRequest;
import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.exception.BusinessRuleViolationException;
import com.adriangniadek.BankingSystem.enums.TransferStatus;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.Transfer;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferServiceImplTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransferServiceImpl transferService;

    private Account sourceAccount;
    private Account targetAccount;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        sourceAccount = account(1L, "500.00", "PLN");
        targetAccount = account(2L, "300.00", "PLN");
    }

    @Test
    void shouldCreateCompletedTransferAndUpdateBalances() {
        CreateTransferRequest request = request(1L, 2L, "100.00", "PLN");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(targetAccount));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            transfer.setId(1L);
            return transfer;
        });

        TransferDTO result = transferService.createTransfer(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(result.createdAt()).isNotNull();
        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("400.00");
        assertThat(targetAccount.getBalance()).isEqualByComparingTo("400.00");
        verify(transferRepository).save(any(Transfer.class));
    }

    @Test
    void shouldLockAccountsInIdentifierOrder() {
        CreateTransferRequest request = request(2L, 1L, "100.00", "PLN");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(targetAccount));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transferService.createTransfer(request);

        InOrder lockOrder = inOrder(accountRepository);
        lockOrder.verify(accountRepository).findByIdForUpdate(1L);
        lockOrder.verify(accountRepository).findByIdForUpdate(2L);
    }

    @Test
    void shouldRejectTransferWithInsufficientFunds() {
        CreateTransferRequest request = request(1L, 2L, "600.00", "PLN");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(targetAccount));

        assertThrows(BusinessRuleViolationException.class, () -> transferService.createTransfer(request));

        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    void shouldRejectTransferBetweenDifferentCurrencies() {
        targetAccount.setCurrency("EUR");
        CreateTransferRequest request = request(1L, 2L, "100.00", "PLN");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(targetAccount));

        assertThrows(BusinessRuleViolationException.class, () -> transferService.createTransfer(request));

        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    void shouldRejectTransferToSameAccountBeforeLocking() {
        CreateTransferRequest request = request(1L, 1L, "100.00", "PLN");

        assertThrows(BusinessRuleViolationException.class, () -> transferService.createTransfer(request));

        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void shouldReturnIncomingAndOutgoingTransfers() {
        Transfer outgoing = transfer(1L, sourceAccount, targetAccount, "100.00");
        Transfer incoming = transfer(2L, targetAccount, sourceAccount, "50.00");
        when(transferRepository.findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(1L, 1L))
                .thenReturn(List.of(incoming, outgoing));

        List<TransferDTO> transfers = transferService.getTransfersForAccount(1L);

        assertThat(transfers).hasSize(2);
        assertThat(transfers.get(0).targetAccountId()).isEqualTo(1L);
        assertThat(transfers.get(1).sourceAccountId()).isEqualTo(1L);
    }

    private Account account(Long id, String balance, String currency) {
        Account account = new Account();
        account.setId(id);
        account.setBalance(new BigDecimal(balance));
        account.setCurrency(currency);
        return account;
    }

    private CreateTransferRequest request(
            Long sourceAccountId, Long targetAccountId, String amount, String currency) {
        return new CreateTransferRequest(
                sourceAccountId, targetAccountId, new BigDecimal(amount), currency, "Test transfer");
    }

    private Transfer transfer(Long id, Account source, Account target, String amount) {
        return new Transfer(
                id,
                source,
                target,
                new BigDecimal(amount),
                "PLN",
                "Test transfer",
                TransferStatus.COMPLETED,
                LocalDateTime.now());
    }
}
