package com.adriangniadek.BankingSystem.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {

    private static final int ACCOUNT_NUMBER_LENGTH = 20;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder accountNumber = new StringBuilder(ACCOUNT_NUMBER_LENGTH);
        accountNumber.append(random.nextInt(1, 10));
        for (int i = 1; i < ACCOUNT_NUMBER_LENGTH; i++) {
            accountNumber.append(random.nextInt(10));
        }
        return accountNumber.toString();
    }
}
