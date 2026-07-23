package com.adriangniadek.BankingSystem.security;

import com.adriangniadek.BankingSystem.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final Instant NOW = Instant.parse("2026-07-22T12:00:00Z");

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        var properties = new JwtProperties(
                "test-only-signing-key-must-be-at-least-32-bytes-long-for-hs256",
                Duration.ofMinutes(15),
                Duration.ofDays(30));
        tokenProvider = new JwtTokenProvider(properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldGenerateValidTokenForUser() {
        String token = tokenProvider.generateAccessToken("user@example.com");

        assertThat(tokenProvider.validateAccessToken(token)).isTrue();
        assertThat(tokenProvider.getUsernameFromJWT(token)).isEqualTo("user@example.com");
        assertThat(tokenProvider.getAccessTokenExpirationSeconds()).isEqualTo(900);
    }

    @Test
    void shouldRejectModifiedToken() {
        String token = tokenProvider.generateAccessToken("user@example.com");
        String[] tokenParts = token.split("\\.");
        char firstSignatureCharacter = tokenParts[2].charAt(0);
        tokenParts[2] = (firstSignatureCharacter == 'a' ? 'b' : 'a') + tokenParts[2].substring(1);
        String modifiedToken = String.join(".", tokenParts);

        assertThat(tokenProvider.validateAccessToken(modifiedToken)).isFalse();
    }

    @Test
    void shouldRejectExpiredAccessToken() {
        String token = tokenProvider.generateAccessToken("user@example.com");
        var properties = new JwtProperties(
                "test-only-signing-key-must-be-at-least-32-bytes-long-for-hs256",
                Duration.ofMinutes(15),
                Duration.ofDays(30));
        var futureProvider = new JwtTokenProvider(
                properties,
                Clock.fixed(NOW.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));

        assertThat(futureProvider.validateAccessToken(token)).isFalse();
    }
}
