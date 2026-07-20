package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.AccountEntryDTO;
import com.adriangniadek.BankingSystem.dto.AccountStatementDTO;
import com.adriangniadek.BankingSystem.dto.CreateAccountRequest;
import com.adriangniadek.BankingSystem.dto.CreateDepositRequest;
import com.adriangniadek.BankingSystem.dto.UpdateAccountStatusRequest;
import com.adriangniadek.BankingSystem.enums.AccountEntryType;
import com.adriangniadek.BankingSystem.enums.AccountStatus;
import com.adriangniadek.BankingSystem.enums.AccountType;
import com.adriangniadek.BankingSystem.exception.BusinessRuleViolationException;
import com.adriangniadek.BankingSystem.exception.ResourceNotFoundException;
import com.adriangniadek.BankingSystem.mapper.AccountEntryMapper;
import com.adriangniadek.BankingSystem.mapper.AccountMapper;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.AccountEntry;
import com.adriangniadek.BankingSystem.model.User;
import com.adriangniadek.BankingSystem.repository.AccountEntryRepository;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.UserRepository;
import com.adriangniadek.BankingSystem.service.AccountNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountEntryRepository accountEntryRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    private AccountServiceImpl accountService;
    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accountService = new AccountServiceImpl(
                accountRepository,
                userRepository,
                accountEntryRepository,
                new AccountMapper(),
                new AccountEntryMapper(),
                accountNumberGenerator);

        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john@example.com");

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setAccountNumber("12345678901234567890");
        testAccount.setAccountType(AccountType.SAVINGS);
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setCurrency("USD");
        testAccount.setStatus(AccountStatus.ACTIVE);
        testAccount.setUser(testUser);
    }

    @Test
    void shouldCreateAccountWithGeneratedNumberAndZeroBalance() {
        CreateAccountRequest request = new CreateAccountRequest(AccountType.SAVINGS, "USD");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountNumberGenerator.generate()).thenReturn("12345678901234567890");
        when(accountRepository.findByAccountNumber("12345678901234567890")).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(1L);
            return account;
        });

        AccountDTO createdAccount = accountService.createAccount(1L, request);

        assertThat(createdAccount.accountNumber()).isEqualTo("12345678901234567890");
        assertThat(createdAccount.accountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(createdAccount.balance()).isEqualByComparingTo("0.00");
        assertThat(createdAccount.status()).isEqualTo(AccountStatus.ACTIVE);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getUser()).isEqualTo(testUser);
    }

    @Test
    void shouldCreateAccountForCurrentUser() {
        CreateAccountRequest request = new CreateAccountRequest(AccountType.CHECKING, "PLN");
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(accountNumberGenerator.generate()).thenReturn("12345678901234567890");
        when(accountRepository.findByAccountNumber("12345678901234567890")).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountDTO createdAccount = accountService.createCurrentUserAccount(testUser.getEmail(), request);

        assertThat(createdAccount.userId()).isEqualTo(testUser.getId());
        assertThat(createdAccount.accountType()).isEqualTo(AccountType.CHECKING);
        verify(userRepository).findByEmail(testUser.getEmail());
    }

    @Test
    void shouldRejectCurrentAccountCreationForMissingUser() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.createCurrentUserAccount(
                        "missing@example.com", new CreateAccountRequest(AccountType.CHECKING, "PLN")));
    }

    @Test
    void shouldGenerateAnotherNumberWhenFirstAlreadyExists() {
        CreateAccountRequest request = new CreateAccountRequest(AccountType.CHECKING, "PLN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountNumberGenerator.generate())
                .thenReturn("11111111111111111111", "22222222222222222222");
        when(accountRepository.findByAccountNumber("11111111111111111111"))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.findByAccountNumber("22222222222222222222"))
                .thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountDTO createdAccount = accountService.createAccount(1L, request);

        assertThat(createdAccount.accountNumber()).isEqualTo("22222222222222222222");
    }

    @Test
    void shouldGetOnlyUserAccounts() {
        when(accountRepository.findByUserId(1L)).thenReturn(List.of(testAccount));

        List<AccountDTO> accounts = accountService.getUserAccounts(1L);

        assertThat(accounts).hasSize(1);
        assertThat(accounts.getFirst().accountNumber()).isEqualTo("12345678901234567890");
        verify(accountRepository).findByUserId(1L);
    }

    @Test
    void shouldGetCurrentUserAccountsByEmail() {
        when(accountRepository.findByUserEmail(testUser.getEmail())).thenReturn(List.of(testAccount));

        List<AccountDTO> accounts = accountService.getCurrentUserAccounts(testUser.getEmail());

        assertThat(accounts).singleElement()
                .extracting(AccountDTO::accountNumber)
                .isEqualTo(testAccount.getAccountNumber());
        verify(accountRepository).findByUserEmail(testUser.getEmail());
    }

    @Test
    void shouldReturnCorrectBalance() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        BigDecimal balance = accountService.getAccountBalance(1L);

        assertThat(balance).isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldThrowExceptionIfAccountNotFound() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.getAccountBalance(99L));
    }

    @Test
    void shouldDepositFundsAndCreateAuditEntry() {
        CreateDepositRequest request = new CreateDepositRequest(
                UUID.randomUUID(), new BigDecimal("250.00"), "USD", "Cash deposit");
        when(accountRepository.findByIdForUpdate(testAccount.getId())).thenReturn(Optional.of(testAccount));
        when(accountEntryRepository.save(any(AccountEntry.class))).thenAnswer(invocation -> {
            AccountEntry entry = invocation.getArgument(0);
            entry.setId(1L);
            return entry;
        });

        AccountEntryDTO result = accountService.deposit(
                testAccount.getId(), request, "admin@example.com");

        assertThat(testAccount.getBalance()).isEqualByComparingTo("1250.00");
        assertThat(result.type()).isEqualTo(AccountEntryType.DEPOSIT);

        ArgumentCaptor<AccountEntry> entryCaptor = ArgumentCaptor.forClass(AccountEntry.class);
        verify(accountEntryRepository).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getCreatedBy()).isEqualTo("admin@example.com");
        assertThat(entryCaptor.getValue().getIdempotencyKey())
                .isEqualTo(request.idempotencyKey().toString());
    }

    @Test
    void shouldReturnExistingDepositWithoutChangingBalanceAgain() {
        CreateDepositRequest request = new CreateDepositRequest(
                UUID.randomUUID(), new BigDecimal("250.00"), "USD", "Cash deposit");
        AccountEntry existingEntry = entry(
                1L, AccountEntryType.DEPOSIT, "250.00", LocalDateTime.now());
        existingEntry.setDescription(request.description());
        existingEntry.setIdempotencyKey(request.idempotencyKey().toString());
        when(accountRepository.findByIdForUpdate(testAccount.getId())).thenReturn(Optional.of(testAccount));
        when(accountEntryRepository.findByIdempotencyKey(request.idempotencyKey().toString()))
                .thenReturn(Optional.of(existingEntry));

        accountService.deposit(testAccount.getId(), request, "admin@example.com");

        assertThat(testAccount.getBalance()).isEqualByComparingTo("1000.00");
        verify(accountEntryRepository, never()).save(any());
    }

    @Test
    void shouldRejectDepositToBlockedAccount() {
        testAccount.setStatus(AccountStatus.BLOCKED);
        CreateDepositRequest request = new CreateDepositRequest(
                UUID.randomUUID(), new BigDecimal("250.00"), "USD", "Cash deposit");
        when(accountRepository.findByIdForUpdate(testAccount.getId())).thenReturn(Optional.of(testAccount));

        assertThatThrownBy(() -> accountService.deposit(
                testAccount.getId(), request, "admin@example.com"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Deposits require an active account");

        verify(accountEntryRepository, never()).save(any());
    }

    @Test
    void shouldBlockActiveAccount() {
        when(accountRepository.findByIdForUpdate(testAccount.getId())).thenReturn(Optional.of(testAccount));

        AccountDTO result = accountService.updateAccountStatus(
                testAccount.getId(), new UpdateAccountStatusRequest(AccountStatus.BLOCKED));

        assertThat(result.status()).isEqualTo(AccountStatus.BLOCKED);
        assertThat(testAccount.getStatus()).isEqualTo(AccountStatus.BLOCKED);
    }

    @Test
    void shouldRejectClosedStatusInAdministrativeUpdate() {
        when(accountRepository.findByIdForUpdate(testAccount.getId())).thenReturn(Optional.of(testAccount));

        assertThatThrownBy(() -> accountService.updateAccountStatus(
                testAccount.getId(), new UpdateAccountStatusRequest(AccountStatus.CLOSED)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Closed status can only be set by closing the account");
    }

    @Test
    void shouldRejectReopeningClosedAccount() {
        testAccount.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findByIdForUpdate(testAccount.getId())).thenReturn(Optional.of(testAccount));

        assertThatThrownBy(() -> accountService.updateAccountStatus(
                testAccount.getId(), new UpdateAccountStatusRequest(AccountStatus.ACTIVE)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Closed account status cannot be changed");
    }

    @Test
    void shouldCloseAccountWithZeroBalance() {
        testAccount.setBalance(BigDecimal.ZERO.setScale(2));
        when(accountRepository.findByIdForUpdate(testAccount.getId())).thenReturn(Optional.of(testAccount));

        accountService.closeAccount(testAccount.getId());

        assertThat(testAccount.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void shouldRejectClosingAccountWithRemainingBalance() {
        when(accountRepository.findByIdForUpdate(testAccount.getId())).thenReturn(Optional.of(testAccount));

        assertThatThrownBy(() -> accountService.closeAccount(testAccount.getId()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Account balance must be zero before closing");
        assertThat(testAccount.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void shouldCalculateHistoricalStatementBalances() {
        LocalDateTime startDate = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 1, 31, 23, 59);
        testAccount.setBalance(new BigDecimal("130.00"));
        AccountEntry outgoingInRange = entry(
                1L, AccountEntryType.TRANSFER_OUT, "20.00", startDate.plusDays(5));
        AccountEntry incomingInRange = entry(
                2L, AccountEntryType.TRANSFER_IN, "50.00", startDate.plusDays(10));
        AccountEntry outgoingAfterRange = entry(
                3L, AccountEntryType.TRANSFER_OUT, "10.00", endDate.plusDays(1));

        when(accountRepository.findById(testAccount.getId())).thenReturn(Optional.of(testAccount));
        when(accountEntryRepository.findByAccountIdFromDate(testAccount.getId(), startDate))
                .thenReturn(List.of(outgoingInRange, incomingInRange, outgoingAfterRange));

        AccountStatementDTO statement = accountService.generateAccountStatement(
                testAccount.getId(), startDate, endDate);

        assertThat(statement.openingBalance()).isEqualByComparingTo("110.00");
        assertThat(statement.closingBalance()).isEqualByComparingTo("140.00");
        assertThat(statement.entries()).extracting(AccountEntryDTO::id)
                .containsExactly(1L, 2L);
    }

    @Test
    void shouldRejectStatementWithReversedDateRange() {
        LocalDateTime startDate = LocalDateTime.of(2026, 2, 1, 0, 0);
        LocalDateTime endDate = startDate.minusDays(1);

        assertThatThrownBy(() ->
                accountService.generateAccountStatement(testAccount.getId(), startDate, endDate))
                .isInstanceOf(com.adriangniadek.BankingSystem.exception.BusinessRuleViolationException.class)
                .hasMessage("Start date must not be after end date");
    }

    private AccountEntry entry(
            Long id, AccountEntryType type, String amount, LocalDateTime createdAt) {
        AccountEntry entry = new AccountEntry();
        entry.setId(id);
        entry.setAccount(testAccount);
        entry.setType(type);
        entry.setAmount(new BigDecimal(amount));
        entry.setCurrency("USD");
        entry.setDescription("Test operation");
        entry.setCreatedAt(createdAt);
        return entry;
    }
}
