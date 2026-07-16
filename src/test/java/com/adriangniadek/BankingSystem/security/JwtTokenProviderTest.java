package com.adriangniadek.BankingSystem.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(
                tokenProvider,
                "secret",
                "test-only-signing-key-must-be-at-least-32-bytes-long-for-hs256");
        ReflectionTestUtils.setField(tokenProvider, "jwtExpirationInMs", 3_600_000L);
    }

    @Test
    void shouldGenerateValidTokenForUser() {
        String token = tokenProvider.generateToken("user@example.com");

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUsernameFromJWT(token)).isEqualTo("user@example.com");
    }

    @Test
    void shouldRejectModifiedToken() {
        String token = tokenProvider.generateToken("user@example.com");
        String modifiedToken = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertThat(tokenProvider.validateToken(modifiedToken)).isFalse();
    }
}
