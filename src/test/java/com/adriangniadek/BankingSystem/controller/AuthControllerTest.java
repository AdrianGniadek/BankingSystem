package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.config.RefreshCookieProperties;
import com.adriangniadek.BankingSystem.dto.LoginRequest;
import com.adriangniadek.BankingSystem.dto.RegisterRequest;
import com.adriangniadek.BankingSystem.security.JwtTokenProvider;
import com.adriangniadek.BankingSystem.service.AuthenticationService;
import com.adriangniadek.BankingSystem.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RefreshCookieProperties refreshCookieProperties;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUpCookieProperties() {
        when(refreshCookieProperties.name()).thenReturn("refresh_token");
        when(refreshCookieProperties.sameSite()).thenReturn("Strict");
    }

    @Test
    void shouldReturnAccessTokenAndSecureRefreshCookieWhenLoginIsSuccessful() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "password");
        var tokens = new AuthenticationService.SessionTokens(
                "access-token", 900, "refresh-token", 2_592_000);
        when(authenticationService.login(request)).thenReturn(tokens);

        String json = """
                {
                  "email": "test@example.com",
                  "password": "password"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("refresh_token=refresh-token"),
                                org.hamcrest.Matchers.containsString("HttpOnly"),
                                org.hamcrest.Matchers.containsString("SameSite=Strict"),
                                org.hamcrest.Matchers.containsString("Path=/auth"))));
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthenticationFails() throws Exception {
        LoginRequest request = new LoginRequest("wrong@example.com", "wrong-password");
        when(authenticationService.login(request))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        String json = """
                {
                  "email": "wrong@example.com",
                  "password": "wrong-password"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Authentication failed"))
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void shouldRotateRefreshCookieWhenSessionIsRefreshed() throws Exception {
        var tokens = new AuthenticationService.SessionTokens(
                "new-access-token", 900, "new-refresh-token", 2_500_000);
        when(authenticationService.refresh("old-refresh-token")).thenReturn(tokens);

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refresh_token", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("refresh_token=new-refresh-token")));
    }

    @Test
    void shouldRevokeSessionAndClearCookieWhenLoggingOut() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refresh_token", "refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("refresh_token="),
                                org.hamcrest.Matchers.containsString("Max-Age=0"))));

        verify(authenticationService).logout("refresh-token");
    }

    @Test
    void shouldRegisterUserAndReturnCreated() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Jan",
                "Kowalski",
                "jan@example.com",
                "password123",
                "90010112345",
                "123456789"
        );

        String json = """
                {
                  "firstName": "Jan",
                  "lastName": "Kowalski",
                  "email": "jan@example.com",
                  "password": "password123",
                  "pesel": "90010112345",
                  "phoneNumber": "123456789"
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        verify(userService).createUser(request);
    }
}
