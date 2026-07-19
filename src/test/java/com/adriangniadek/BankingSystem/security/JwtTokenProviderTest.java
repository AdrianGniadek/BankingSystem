package com.adriangniadek.BankingSystem.security;

import com.adriangniadek.BankingSystem.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        var properties = new JwtProperties(
                "test-only-signing-key-must-be-at-least-32-bytes-long-for-hs256",
                Duration.ofHours(1));
        tokenProvider = new JwtTokenProvider(properties);
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
        String[] tokenParts = token.split("\\.");
        char firstSignatureCharacter = tokenParts[2].charAt(0);
        tokenParts[2] = (firstSignatureCharacter == 'a' ? 'b' : 'a') + tokenParts[2].substring(1);
        String modifiedToken = String.join(".", tokenParts);

        assertThat(tokenProvider.validateToken(modifiedToken)).isFalse();
    }
}
