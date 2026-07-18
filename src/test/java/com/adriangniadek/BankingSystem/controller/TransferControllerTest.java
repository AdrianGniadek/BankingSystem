package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.CreateTransferRequest;
import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.enums.TransferStatus;
import com.adriangniadek.BankingSystem.security.CustomUserDetailsService;
import com.adriangniadek.BankingSystem.security.JwtTokenProvider;
import com.adriangniadek.BankingSystem.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateTransfer() throws Exception {
        CreateTransferRequest request = new CreateTransferRequest(
                1L,
                2L,
                BigDecimal.valueOf(500),
                "PLN",
                "Payment"
        );

        TransferDTO savedTransfer = new TransferDTO(
                1L,
                1L,
                2L,
                BigDecimal.valueOf(500),
                "PLN",
                "Payment",
                TransferStatus.COMPLETED,
                LocalDateTime.now()
        );

        String mockToken = "mock-jwt-token";
        String testEmail = "test@example.com";

        Mockito.when(jwtTokenProvider.validateToken(mockToken)).thenReturn(true);
        Mockito.when(jwtTokenProvider.getUsernameFromJWT(mockToken)).thenReturn(testEmail);

        Mockito.when(userDetailsService.loadUserByUsername(testEmail))
                .thenReturn(new User(testEmail, "password",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        Mockito.when(transferService.createTransfer(Mockito.any())).thenReturn(savedTransfer);

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + mockToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.amount").value(500));
    }

    @Test
    void shouldRejectInvalidTransferRequest() throws Exception {
        String mockToken = "mock-jwt-token";
        String testEmail = "test@example.com";

        Mockito.when(jwtTokenProvider.validateToken(mockToken)).thenReturn(true);
        Mockito.when(jwtTokenProvider.getUsernameFromJWT(mockToken)).thenReturn(testEmail);
        Mockito.when(userDetailsService.loadUserByUsername(testEmail))
                .thenReturn(new User(testEmail, "password",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        CreateTransferRequest request = new CreateTransferRequest(1L, 2L, BigDecimal.ZERO, "pln", null);

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + mockToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.currency").exists());
    }
}
