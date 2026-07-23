package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.CreateTransferRequest;
import com.adriangniadek.BankingSystem.dto.PageResponse;
import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.enums.AccountEntryType;
import com.adriangniadek.BankingSystem.enums.AccountStatus;
import com.adriangniadek.BankingSystem.enums.TransferStatus;
import com.adriangniadek.BankingSystem.exception.BusinessRuleViolationException;
import com.adriangniadek.BankingSystem.exception.ResourceConflictException;
import com.adriangniadek.BankingSystem.mapper.TransferMapper;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.AccountEntry;
import com.adriangniadek.BankingSystem.model.Transfer;
import com.adriangniadek.BankingSystem.repository.AccountEntryRepository;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountEntryRepository accountEntryRepository;

    @Spy
    private TransferMapper transferMapper = new TransferMapper();

    @InjectMocks
    private TransferServiceImpl transferService;

    private Account sourceAccount;
    private Account targetAccount;

    @BeforeEach
    void setUp() {
        sourceAccount = account(1L, "500.00", "PLN");
        targetAccount = account(2L, "300.00", "PLN");
    }

    @Test
    void shouldCreateCompletedTransferAndLedgerEntries() {
        CreateTransferRequest request = request(1L, 2L, "100.00", "PLN");
        mockLockedAccounts(request);
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            transfer.setId(1L);
            return transfer;
        });

        TransferDTO result = transferService.createTransfer(request, "owner@example.com");

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("400.00");
        assertThat(targetAccount.getBalance()).isEqualByComparingTo("400.00");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AccountEntry>> entriesCaptor = ArgumentCaptor.forClass(List.class);
        verify(accountEntryRepository).saveAll(entriesCaptor.capture());
        assertThat(entriesCaptor.getValue()).extracting(AccountEntry::getType)
                .containsExactly(AccountEntryType.TRANSFER_OUT, AccountEntryType.TRANSFER_IN);
        assertThat(entriesCaptor.getValue()).extracting(AccountEntry::getCreatedBy)
                .containsOnly("owner@example.com");
    }

    @Test
    void shouldReturnExistingTransferForRepeatedRequest() {
        CreateTransferRequest request = request(1L, 2L, "100.00", "PLN");
        Transfer existingTransfer = transfer(1L, sourceAccount, targetAccount, "100.00");
        existingTransfer.setIdempotencyKey(request.idempotencyKey().toString());
        mockLockedAccounts(request);
        when(transferRepository.findByIdempotencyKey(request.idempotencyKey().toString()))
                .thenReturn(Optional.of(existingTransfer));

        TransferDTO result = transferService.createTransfer(request, "owner@example.com");

        assertThat(result.id()).isEqualTo(existingTransfer.getId());
        verify(accountEntryRepository, never()).saveAll(any());
        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("500.00");
        assertThat(targetAccount.getBalance()).isEqualByComparingTo("300.00");
    }

    @Test
    void shouldRejectIdempotencyKeyUsedWithDifferentTransfer() {
        CreateTransferRequest request = request(1L, 2L, "100.00", "PLN");
        Transfer existingTransfer = transfer(1L, sourceAccount, targetAccount, "50.00");
        mockLockedAccounts(request);
        when(transferRepository.findByIdempotencyKey(request.idempotencyKey().toString()))
                .thenReturn(Optional.of(existingTransfer));

        assertThatThrownBy(() -> transferService.createTransfer(request, "owner@example.com"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Idempotency key was already used for another transfer");
    }

    @Test
    void shouldLockAccountsInIdentifierOrder() {
        CreateTransferRequest request = request(2L, 1L, "100.00", "PLN");
        mockLockedAccounts(request);
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transferService.createTransfer(request, "owner@example.com");

        InOrder lockOrder = inOrder(accountRepository);
        lockOrder.verify(accountRepository).findByIdForUpdate(1L);
        lockOrder.verify(accountRepository).findByIdForUpdate(2L);
    }

    @Test
    void shouldRejectTransferWithInsufficientFunds() {
        CreateTransferRequest request = request(1L, 2L, "600.00", "PLN");
        mockLockedAccounts(request);

        assertThatThrownBy(() -> transferService.createTransfer(request, "owner@example.com"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Insufficient funds in source account");

        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    void shouldRejectTransferBetweenDifferentCurrencies() {
        targetAccount.setCurrency("EUR");
        CreateTransferRequest request = request(1L, 2L, "100.00", "PLN");
        mockLockedAccounts(request);

        assertThatThrownBy(() -> transferService.createTransfer(request, "owner@example.com"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Transfer currency must match both account currencies");
    }

    @Test
    void shouldRejectTransferFromBlockedAccount() {
        sourceAccount.setStatus(AccountStatus.BLOCKED);
        CreateTransferRequest request = request(1L, 2L, "100.00", "PLN");
        mockLockedAccounts(request);

        assertThatThrownBy(() -> transferService.createTransfer(request, "owner@example.com"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Source account must be active");
    }

    @Test
    void shouldRejectTransferToClosedAccount() {
        targetAccount.setStatus(AccountStatus.CLOSED);
        CreateTransferRequest request = request(1L, 2L, "100.00", "PLN");
        mockLockedAccounts(request);

        assertThatThrownBy(() -> transferService.createTransfer(request, "owner@example.com"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Target account must be active");
    }

    @Test
    void shouldRejectTransferToSameAccountBeforeLocking() {
        CreateTransferRequest request = request(1L, 1L, "100.00", "PLN");
        when(accountRepository.findIdByAccountNumber(request.targetAccountNumber()))
                .thenReturn(Optional.of(sourceAccount.getId()));

        assertThatThrownBy(() -> transferService.createTransfer(request, "owner@example.com"))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void shouldRejectTransferToUnknownAccountNumber() {
        CreateTransferRequest request = request(1L, 2L, "100.00", "PLN");
        when(accountRepository.findIdByAccountNumber(request.targetAccountNumber()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.createTransfer(request, "owner@example.com"))
                .isInstanceOf(com.adriangniadek.BankingSystem.exception.ResourceNotFoundException.class)
                .hasMessage("Target account not found");

        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void shouldReturnPaginatedIncomingAndOutgoingTransfers() {
        Transfer outgoing = transfer(1L, sourceAccount, targetAccount, "100.00");
        Transfer incoming = transfer(2L, targetAccount, sourceAccount, "50.00");
        when(transferRepository.findBySourceAccountIdOrTargetAccountId(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(1L),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(incoming, outgoing)));

        PageResponse<TransferDTO> transfers = transferService.getTransfersForAccount(1L, 0, 20);

        assertThat(transfers.content()).hasSize(2);
        assertThat(transfers.content().getFirst().targetAccountNumber())
                .isEqualTo(sourceAccount.getAccountNumber());
        assertThat(transfers.page()).isZero();
        assertThat(transfers.totalElements()).isEqualTo(2);
    }

    private void mockLockedAccounts(CreateTransferRequest request) {
        Account target = request.targetAccountNumber().equals(sourceAccount.getAccountNumber())
                ? sourceAccount
                : targetAccount;
        when(accountRepository.findIdByAccountNumber(request.targetAccountNumber()))
                .thenReturn(Optional.of(target.getId()));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(targetAccount));
    }

    private Account account(Long id, String balance, String currency) {
        Account account = new Account();
        account.setId(id);
        account.setAccountNumber(id == 1L
                ? "11111111111111111111"
                : "22222222222222222222");
        account.setBalance(new BigDecimal(balance));
        account.setCurrency(currency);
        account.setStatus(AccountStatus.ACTIVE);
        return account;
    }

    private CreateTransferRequest request(
            Long sourceAccountId, Long targetAccountId, String amount, String currency) {
        return new CreateTransferRequest(
                UUID.randomUUID(),
                sourceAccountId,
                targetAccountId == 1L
                        ? "11111111111111111111"
                        : "22222222222222222222",
                new BigDecimal(amount),
                currency,
                "Test transfer");
    }

    private Transfer transfer(Long id, Account source, Account target, String amount) {
        Transfer transfer = new Transfer();
        transfer.setId(id);
        transfer.setSourceAccount(source);
        transfer.setTargetAccount(target);
        transfer.setAmount(new BigDecimal(amount));
        transfer.setCurrency("PLN");
        transfer.setDescription("Test transfer");
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setCreatedAt(LocalDateTime.now());
        return transfer;
    }
}
