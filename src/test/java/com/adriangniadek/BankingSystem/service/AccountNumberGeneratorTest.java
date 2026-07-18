package com.adriangniadek.BankingSystem.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountNumberGeneratorTest {

    private final AccountNumberGenerator generator = new AccountNumberGenerator();

    @Test
    void shouldGenerateTwentyDigitAccountNumberWithoutLeadingZero() {
        String accountNumber = generator.generate();

        assertThat(accountNumber).matches("[1-9][0-9]{19}");
    }
}
