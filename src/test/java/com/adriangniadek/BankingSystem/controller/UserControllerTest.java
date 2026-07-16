package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.UserDTO;
import com.adriangniadek.BankingSystem.exception.ResourceConflictException;
import com.adriangniadek.BankingSystem.security.JwtAuthFilter;
import com.adriangniadek.BankingSystem.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void shouldCreateUserSuccessfully() throws Exception {
        UserDTO user = new UserDTO(
                null,
                "Jan",
                "Kowalski",
                "jan.kowalski@example.com",
                Set.of("USER")
        );

        UserDTO savedUser = new UserDTO(
                1L,
                user.firstName(),
                user.lastName(),
                user.email(),
                Set.of("ROLE_USER")
        );
        Mockito.when(userService.createUser(any(UserDTO.class), eq("secret123"))).thenReturn(savedUser);

        mockMvc.perform(post("/users")
                        .param("password", "secret123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("jan.kowalski@example.com"));
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        UserDTO user = new UserDTO(
                null,
                "Jan",
                "Kowalski",
                "jan.kowalski@example.com",
                Set.of("USER")
        );
        Mockito.when(userService.createUser(any(UserDTO.class), eq("secret123")))
                .thenThrow(new ResourceConflictException("Email already in use"));

        mockMvc.perform(post("/users")
                        .param("password", "secret123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource conflict"))
                .andExpect(jsonPath("$.detail").value("Email already in use"));
    }
}
