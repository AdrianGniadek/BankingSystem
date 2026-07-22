package com.adriangniadek.BankingSystem.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.refresh-cookie")
public record RefreshCookieProperties(
        @NotBlank String name,
        boolean secure,
        @NotBlank String sameSite
) {
}
