package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.User;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.TransferRepository;
import com.adriangniadek.BankingSystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransferRepository transferRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john@example.com");

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setAccountNumber("1234567890");
        testAccount.setAccountType("SAVINGS");
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setCurrency("USD");
        testAccount.setUser(testUser);
    }

    @Test
    void shouldCreateAccountSuccessfully() {
        AccountDTO accountDTO = new AccountDTO(null, "1234567890", "SAVINGS", new BigDecimal("1000.00"), "USD", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountDTO createdAccount = accountService.createAccount(1L, accountDTO);

        assertNotNull(createdAccount);
        assertEquals("1234567890", createdAccount.accountNumber());
        assertEquals("SAVINGS", createdAccount.accountType());
        assertEquals(new BigDecimal("1000.00"), createdAccount.balance());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void shouldGetUserAccounts() {
        when(accountRepository.findAll()).thenReturn(List.of(testAccount));

        List<AccountDTO> accounts = accountService.getUserAccounts(1L);

        assertEquals(1, accounts.size());
        assertEquals("1234567890", accounts.get(0).accountNumber());
    }

    @Test
    void shouldReturnCorrectBalance() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        BigDecimal balance = accountService.getAccountBalance(1L);
        assertEquals(new BigDecimal("1000.00"), balance);
    }

    @Test
    void shouldThrowExceptionIfAccountNotFound() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> accountService.getAccountBalance(99L));
    }
}
