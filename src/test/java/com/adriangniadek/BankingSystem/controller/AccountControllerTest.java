package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAccount() throws Exception {
        AccountDTO dto = new AccountDTO(1L, "PL123456789", "SAVINGS", BigDecimal.ZERO, "PLN", 1L);
        Mockito.when(accountService.createAccount(eq(1L), any(AccountDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/accounts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("PL123456789"));
    }

    @Test
    void shouldReturnUserAccounts() throws Exception {
        List<AccountDTO> accounts = List.of(
                new AccountDTO(1L, "PL111", "SAVINGS", BigDecimal.valueOf(1000), "PLN", 1L),
                new AccountDTO(2L, "PL222", "CHECKING", BigDecimal.valueOf(500), "PLN", 1L)
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
        AccountDTO invalidAccount = new AccountDTO(
                null, "123", "SAVINGS", BigDecimal.valueOf(-1), "pln", 0L);

        mockMvc.perform(post("/accounts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidAccount)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.accountNumber").exists())
                .andExpect(jsonPath("$.errors.balance").exists())
                .andExpect(jsonPath("$.errors.currency").exists())
                .andExpect(jsonPath("$.errors.userId").exists());
    }

}
