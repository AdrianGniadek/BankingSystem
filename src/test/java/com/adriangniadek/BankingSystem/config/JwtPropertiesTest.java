package com.adriangniadek.BankingSystem.config;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesTest {

    @Test
    void shouldAcceptSecureConfiguration() {
        var properties = new JwtProperties(
                "test-only-signing-key-must-be-at-least-32-bytes-long-for-hs256",
                Duration.ofMinutes(15),
                Duration.ofDays(30));

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(properties)).isEmpty();
        }
    }

    @Test
    void shouldRejectWeakSecretAndInvalidExpiration() {
        var properties = new JwtProperties("too-short", Duration.ZERO, Duration.ofSeconds(-1));

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(properties))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder(
                            "secret",
                            "accessTokenExpirationValid",
                            "refreshTokenExpirationValid");
        }
    }
}
