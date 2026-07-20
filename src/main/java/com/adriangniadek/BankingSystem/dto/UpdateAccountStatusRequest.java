package com.adriangniadek.BankingSystem.dto;

import com.adriangniadek.BankingSystem.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @NotNull(message = "Account status is required")
        AccountStatus status) {
}
