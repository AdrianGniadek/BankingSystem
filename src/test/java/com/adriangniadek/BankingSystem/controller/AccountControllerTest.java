package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.security.JwtTokenProvider;
import com.adriangniadek.BankingSystem.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

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
    void shouldReturnTransactionHistory() throws Exception {
        List<TransferDTO> transfers = List.of(
                new TransferDTO(1L,1L,2L, BigDecimal.valueOf(100), "USD", "Test transfer",
                        "COMPLETED", LocalDateTime.parse("2024-04-20T12:00:00")
                )
        );

        Mockito.when(accountService.getAccountTransactionHistory(1L)).thenReturn(transfers);

        mockMvc.perform(get("/accounts/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].amount").value(100));
    }
}
