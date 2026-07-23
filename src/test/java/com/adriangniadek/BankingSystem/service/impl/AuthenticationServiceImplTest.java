package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.config.JwtProperties;
import com.adriangniadek.BankingSystem.dto.LoginRequest;
import com.adriangniadek.BankingSystem.exception.InvalidRefreshTokenException;
import com.adriangniadek.BankingSystem.model.RefreshToken;
import com.adriangniadek.BankingSystem.model.User;
import com.adriangniadek.BankingSystem.repository.RefreshTokenRepository;
import com.adriangniadek.BankingSystem.repository.UserRepository;
import com.adriangniadek.BankingSystem.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-07-22T12:00:00Z");
    private static final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(30);

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    private AuthenticationServiceImpl authenticationService;
    private User user;

    @BeforeEach
    void setUp() {
        var properties = new JwtProperties(
                "test-only-signing-key-must-be-at-least-32-bytes-long-for-hs256",
                ACCESS_TOKEN_EXPIRATION,
                REFRESH_TOKEN_EXPIRATION);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        authenticationService = new AuthenticationServiceImpl(
                authenticationManager,
                jwtTokenProvider,
                properties,
                refreshTokenRepository,
                userRepository,
                clock);

        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
    }

    @Test
    void shouldCreateHashedRefreshTokenWhenUserLogsIn() {
        LoginRequest request = new LoginRequest(user.getEmail(), "password123");
        var authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(user.getEmail())).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);

        var tokens = authenticationService.login(request);

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.accessTokenExpiresIn()).isEqualTo(900);
        assertThat(tokens.refreshToken()).hasSize(43);
        assertThat(tokens.refreshTokenExpiresIn()).isEqualTo(REFRESH_TOKEN_EXPIRATION.toSeconds());

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getTokenHash()).isEqualTo(sha256(tokens.refreshToken()));
        assertThat(savedToken.getTokenHash()).doesNotContain(tokens.refreshToken());
        assertThat(savedToken.getUser()).isSameAs(user);
        assertThat(savedToken.getFamilyId()).isNotBlank();
        assertThat(savedToken.getCreatedAt()).isEqualTo(NOW);
        assertThat(savedToken.getExpiresAt()).isEqualTo(NOW.plus(REFRESH_TOKEN_EXPIRATION));
    }

    @Test
    void shouldRotateActiveRefreshTokenWithoutExtendingSessionLifetime() {
        String rawToken = "current-refresh-token";
        Instant familyExpiration = NOW.plus(Duration.ofDays(10));
        RefreshToken currentToken = refreshToken(
                sha256(rawToken), "family-id", familyExpiration, null);
        when(refreshTokenRepository.findByTokenHashForUpdate(sha256(rawToken)))
                .thenReturn(Optional.of(currentToken));
        when(jwtTokenProvider.generateAccessToken(user.getEmail())).thenReturn("new-access-token");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);

        var tokens = authenticationService.refresh(rawToken);

        assertThat(currentToken.getRevokedAt()).isEqualTo(NOW);
        assertThat(tokens.accessToken()).isEqualTo("new-access-token");
        assertThat(tokens.refreshToken()).isNotEqualTo(rawToken);
        assertThat(tokens.refreshTokenExpiresIn()).isEqualTo(Duration.ofDays(10).toSeconds());

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken replacement = tokenCaptor.getValue();
        assertThat(replacement.getFamilyId()).isEqualTo(currentToken.getFamilyId());
        assertThat(replacement.getExpiresAt()).isEqualTo(familyExpiration);
        assertThat(replacement.getTokenHash()).isEqualTo(sha256(tokens.refreshToken()));
    }

    @Test
    void shouldRevokeTokenFamilyWhenRevokedTokenIsReused() {
        String rawToken = "already-used-refresh-token";
        RefreshToken reusedToken = refreshToken(
                sha256(rawToken), "family-id", NOW.plus(Duration.ofDays(1)), NOW.minusSeconds(1));
        when(refreshTokenRepository.findByTokenHashForUpdate(sha256(rawToken)))
                .thenReturn(Optional.of(reusedToken));

        assertThatThrownBy(() -> authenticationService.refresh(rawToken))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token");

        verify(refreshTokenRepository).revokeActiveFamily("family-id", NOW);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldRejectAndRevokeExpiredRefreshToken() {
        String rawToken = "expired-refresh-token";
        RefreshToken expiredToken = refreshToken(sha256(rawToken), "family-id", NOW, null);
        when(refreshTokenRepository.findByTokenHashForUpdate(sha256(rawToken)))
                .thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authenticationService.refresh(rawToken))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(expiredToken.getRevokedAt()).isEqualTo(NOW);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldRevokeEntireSessionFamilyWhenUserLogsOut() {
        String rawToken = "refresh-token";
        RefreshToken token = refreshToken(
                sha256(rawToken), "family-id", NOW.plus(Duration.ofDays(1)), null);
        when(refreshTokenRepository.findByTokenHashForUpdate(sha256(rawToken)))
                .thenReturn(Optional.of(token));

        authenticationService.logout(rawToken);

        verify(refreshTokenRepository).revokeActiveFamily("family-id", NOW);
    }

    @Test
    void shouldIgnoreLogoutWithoutRefreshToken() {
        authenticationService.logout(null);

        verify(refreshTokenRepository, never()).findByTokenHashForUpdate(any());
    }

    private RefreshToken refreshToken(
            String tokenHash, String familyId, Instant expiresAt, Instant revokedAt) {
        return new RefreshToken(1L, user, tokenHash, familyId, expiresAt, NOW.minusSeconds(60), revokedAt);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
