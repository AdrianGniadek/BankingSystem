package com.adriangniadek.BankingSystem.dto;

import com.adriangniadek.BankingSystem.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(
        @NotNull(message = "Account type is required")
        AccountType accountType,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter uppercase ISO code")
        String currency) {
}
