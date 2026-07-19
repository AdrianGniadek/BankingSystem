package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.AccountStatementDTO;
import com.adriangniadek.BankingSystem.dto.CreateAccountRequest;
import com.adriangniadek.BankingSystem.enums.AccountType;
import com.adriangniadek.BankingSystem.enums.TransferStatus;
import com.adriangniadek.BankingSystem.exception.ResourceNotFoundException;
import com.adriangniadek.BankingSystem.mapper.AccountMapper;
import com.adriangniadek.BankingSystem.mapper.TransferMapper;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.Transfer;
import com.adriangniadek.BankingSystem.model.User;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.TransferRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransferRepository transferRepository;

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
                transferRepository,
                new AccountMapper(),
                new TransferMapper(),
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
    void shouldCalculateHistoricalStatementBalances() {
        LocalDateTime startDate = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 1, 31, 23, 59);
        Account otherAccount = new Account();
        otherAccount.setId(2L);

        testAccount.setBalance(new BigDecimal("130.00"));
        Transfer outgoingInRange = transfer(
                1L, testAccount, otherAccount, "20.00", startDate.plusDays(5));
        Transfer incomingInRange = transfer(
                2L, otherAccount, testAccount, "50.00", startDate.plusDays(10));
        Transfer outgoingAfterRange = transfer(
                3L, testAccount, otherAccount, "10.00", endDate.plusDays(1));

        when(accountRepository.findById(testAccount.getId())).thenReturn(Optional.of(testAccount));
        when(transferRepository.findByAccountIdFromDate(
                testAccount.getId(), startDate, TransferStatus.COMPLETED))
                .thenReturn(List.of(outgoingInRange, incomingInRange, outgoingAfterRange));

        AccountStatementDTO statement = accountService.generateAccountStatement(
                testAccount.getId(), startDate, endDate);

        assertThat(statement.openingBalance()).isEqualByComparingTo("110.00");
        assertThat(statement.closingBalance()).isEqualByComparingTo("140.00");
        assertThat(statement.transactions()).extracting(transferDto -> transferDto.id())
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

    private Transfer transfer(
            Long id,
            Account sourceAccount,
            Account targetAccount,
            String amount,
            LocalDateTime createdAt) {
        Transfer transfer = new Transfer();
        transfer.setId(id);
        transfer.setSourceAccount(sourceAccount);
        transfer.setTargetAccount(targetAccount);
        transfer.setAmount(new BigDecimal(amount));
        transfer.setCurrency("USD");
        transfer.setDescription("Test transfer");
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setCreatedAt(createdAt);
        return transfer;
    }
}
