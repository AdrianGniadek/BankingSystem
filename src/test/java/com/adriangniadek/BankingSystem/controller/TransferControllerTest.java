package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.security.CustomUserDetailsService;
import com.adriangniadek.BankingSystem.security.JwtTokenProvider;
import com.adriangniadek.BankingSystem.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransferService transferService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateTransfer() throws Exception {
        TransferDTO transferDTO = new TransferDTO(
                null,
                1L,
                2L,
                BigDecimal.valueOf(500),
                "PLN",
                "Payment",
                "COMPLETED",
                LocalDateTime.now()
        );

        TransferDTO savedTransfer = new TransferDTO(
                1L,
                1L,
                2L,
                BigDecimal.valueOf(500),
                "PLN",
                "Payment",
                "COMPLETED",
                LocalDateTime.now()
        );

        Mockito.when(transferService.getTransfersForAccount(Mockito.any())).thenReturn(Collections.singletonList(savedTransfer));

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
                        .content(objectMapper.writeValueAsString(transferDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.amount").value(500));
    }
}