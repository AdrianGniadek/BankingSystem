package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.config.JwtProperties;
import com.adriangniadek.BankingSystem.dto.LoginRequest;
import com.adriangniadek.BankingSystem.exception.InvalidRefreshTokenException;
import com.adriangniadek.BankingSystem.model.RefreshToken;
import com.adriangniadek.BankingSystem.model.User;
import com.adriangniadek.BankingSystem.repository.RefreshTokenRepository;
import com.adriangniadek.BankingSystem.repository.UserRepository;
import com.adriangniadek.BankingSystem.security.JwtTokenProvider;
import com.adriangniadek.BankingSystem.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Override
    @Transactional
    public SessionTokens login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Instant now = clock.instant();
        Instant refreshTokenExpiresAt = now.plus(jwtProperties.refreshTokenExpiration());
        return createSessionTokens(user, UUID.randomUUID().toString(), refreshTokenExpiresAt, now);
    }

    @Override
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public SessionTokens refresh(String rawRefreshToken) {
        RefreshToken currentToken = findForUpdate(rawRefreshToken);
        Instant now = clock.instant();

        if (currentToken.isRevoked()) {
            refreshTokenRepository.revokeActiveFamily(currentToken.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (!currentToken.getExpiresAt().isAfter(now)) {
            currentToken.setRevokedAt(now);
            throw new InvalidRefreshTokenException();
        }

        currentToken.setRevokedAt(now);
        return createSessionTokens(
                currentToken.getUser(),
                currentToken.getFamilyId(),
                currentToken.getExpiresAt(),
                now);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHashForUpdate(hash(rawRefreshToken))
                .ifPresent(token -> refreshTokenRepository.revokeActiveFamily(
                        token.getFamilyId(), clock.instant()));
    }

    private RefreshToken findForUpdate(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        return refreshTokenRepository.findByTokenHashForUpdate(hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);
    }

    private SessionTokens createSessionTokens(
            User user, String familyId, Instant refreshTokenExpiresAt, Instant now) {
        String rawRefreshToken = generateRefreshToken();
        RefreshToken refreshToken = new RefreshToken(
                null,
                user,
                hash(rawRefreshToken),
                familyId,
                refreshTokenExpiresAt,
                now,
                null);
        refreshTokenRepository.save(refreshToken);

        return new SessionTokens(
                jwtTokenProvider.generateAccessToken(user.getEmail()),
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                rawRefreshToken,
                Math.max(0, Duration.between(now, refreshTokenExpiresAt).toSeconds()));
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
