package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.CreateAccountRequest;
import com.adriangniadek.BankingSystem.enums.AccountType;
import com.adriangniadek.BankingSystem.exception.ResourceNotFoundException;
import com.adriangniadek.BankingSystem.mapper.AccountMapper;
import com.adriangniadek.BankingSystem.model.Account;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
}
