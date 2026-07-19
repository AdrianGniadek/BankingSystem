package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.AccountEntryDTO;
import com.adriangniadek.BankingSystem.dto.CreateAccountRequest;
import com.adriangniadek.BankingSystem.dto.CreateDepositRequest;
import com.adriangniadek.BankingSystem.enums.AccountEntryType;
import com.adriangniadek.BankingSystem.enums.AccountType;
import com.adriangniadek.BankingSystem.exception.ResourceNotFoundException;
import com.adriangniadek.BankingSystem.security.JwtAuthFilter;
import com.adriangniadek.BankingSystem.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    private static final String EMAIL = "john@example.com";
    private static final TestingAuthenticationToken AUTHENTICATION =
            new TestingAuthenticationToken(EMAIL, null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAccountForCurrentUser() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(AccountType.CHECKING, "PLN");
        AccountDTO account = new AccountDTO(
                1L, "12345678901234567890", AccountType.CHECKING, BigDecimal.ZERO, "PLN", 1L);
        Mockito.when(accountService.createCurrentUserAccount(eq(EMAIL), any(CreateAccountRequest.class)))
                .thenReturn(account);

        mockMvc.perform(post("/accounts/me")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("12345678901234567890"));
    }

    @Test
    void shouldReturnCurrentUserAccounts() throws Exception {
        Mockito.when(accountService.getCurrentUserAccounts(EMAIL)).thenReturn(List.of(
                new AccountDTO(
                        1L, "12345678901234567890", AccountType.CHECKING,
                        BigDecimal.ZERO, "PLN", 1L)));

        mockMvc.perform(get("/accounts/me").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].currency").value("PLN"));
    }

    @Test
    void shouldDepositFunds() throws Exception {
        CreateDepositRequest request = new CreateDepositRequest(
                UUID.randomUUID(), new BigDecimal("100.00"), "PLN", "Cash deposit");
        AccountEntryDTO entry = new AccountEntryDTO(
                1L,
                AccountEntryType.DEPOSIT,
                request.amount(),
                request.currency(),
                request.description(),
                LocalDateTime.now(),
                null);
        Mockito.when(accountService.deposit(eq(1L), eq(request), eq(EMAIL))).thenReturn(entry);

        mockMvc.perform(post("/accounts/1/deposits")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(100.0));
    }

    @Test
    void shouldCreateAccount() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(AccountType.SAVINGS, "PLN");
        AccountDTO dto = new AccountDTO(
                1L, "12345678901234567890", AccountType.SAVINGS, BigDecimal.ZERO, "PLN", 1L);
        Mockito.when(accountService.createAccount(eq(1L), any(CreateAccountRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/accounts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("12345678901234567890"));
    }

    @Test
    void shouldReturnUserAccounts() throws Exception {
        List<AccountDTO> accounts = List.of(
                new AccountDTO(1L, "PL111", AccountType.SAVINGS, BigDecimal.valueOf(1000), "PLN", 1L),
                new AccountDTO(2L, "PL222", AccountType.CHECKING, BigDecimal.valueOf(500), "PLN", 1L)
        );

        Mockito.when(accountService.getUserAccounts(1L)).thenReturn(accounts);

        mockMvc.perform(get("/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].accountNumber").value("PL111"));
    }

    @Test
    void shouldReturnAccountBalance() throws Exception {
        Mockito.when(accountService.getAccountBalance(1L)).thenReturn(BigDecimal.valueOf(1234.56));

        mockMvc.perform(get("/accounts/balance/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("1234.56"));
    }

    @Test
    void shouldReturnProblemWhenAccountIsNotFound() throws Exception {
        Mockito.when(accountService.getAccountBalance(99L))
                .thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(get("/accounts/balance/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Account not found"))
                .andExpect(jsonPath("$.instance").value("/accounts/balance/99"));
    }

    @Test
    void shouldReturnValidationErrorsForInvalidAccount() throws Exception {
        CreateAccountRequest invalidRequest = new CreateAccountRequest(null, "pln");

        mockMvc.perform(post("/accounts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.accountType").exists())
                .andExpect(jsonPath("$.errors.currency").exists());
    }

}
