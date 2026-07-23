package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.config.RefreshCookieProperties;
import com.adriangniadek.BankingSystem.dto.LoginRequest;
import com.adriangniadek.BankingSystem.dto.RegisterRequest;
import com.adriangniadek.BankingSystem.dto.TokenResponse;
import com.adriangniadek.BankingSystem.service.AuthenticationService;
import com.adriangniadek.BankingSystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_PATH = "/auth";

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final RefreshCookieProperties refreshCookieProperties;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> authenticateUser(@RequestBody @Valid LoginRequest request) {
        AuthenticationService.SessionTokens tokens = authenticationService.login(request);
        return tokenResponse(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshSession(
            @CookieValue(name = "${security.refresh-cookie.name}", required = false) String refreshToken) {
        AuthenticationService.SessionTokens tokens = authenticationService.refresh(refreshToken);
        return tokenResponse(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${security.refresh-cookie.name}", required = false) String refreshToken) {
        authenticationService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private ResponseEntity<TokenResponse> tokenResponse(AuthenticationService.SessionTokens tokens) {
        TokenResponse response = new TokenResponse(tokens.accessToken(), tokens.accessTokenExpiresIn());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }

    private ResponseCookie refreshCookie(AuthenticationService.SessionTokens tokens) {
        return refreshCookie(tokens.refreshToken(), tokens.refreshTokenExpiresIn());
    }

    private ResponseCookie expiredRefreshCookie() {
        return refreshCookie("", 0);
    }

    private ResponseCookie refreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(refreshCookieProperties.name(), value)
                .httpOnly(true)
                .secure(refreshCookieProperties.secure())
                .sameSite(refreshCookieProperties.sameSite())
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .build();
    }
}
