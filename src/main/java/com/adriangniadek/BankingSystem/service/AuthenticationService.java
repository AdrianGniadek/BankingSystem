package com.adriangniadek.BankingSystem.service;

import com.adriangniadek.BankingSystem.dto.LoginRequest;

public interface AuthenticationService {

    SessionTokens login(LoginRequest request);

    SessionTokens refresh(String refreshToken);

    void logout(String refreshToken);

    record SessionTokens(
            String accessToken,
            long accessTokenExpiresIn,
            String refreshToken,
            long refreshTokenExpiresIn
    ) {
    }
}
