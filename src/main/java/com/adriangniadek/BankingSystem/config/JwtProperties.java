package com.adriangniadek.BankingSystem.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        @NotBlank
        @Size(min = 32, message = "JWT secret must contain at least 32 characters")
        String secret,

        @NotNull
        Duration expiration
) {

    @AssertTrue(message = "JWT expiration must be greater than zero")
    public boolean isExpirationValid() {
        return expiration != null && !expiration.isZero() && !expiration.isNegative();
    }
}
