package com.adriangniadek.BankingSystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 25, message = "First name must be between 2 and 25 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 25, message = "Last name must be between 2 and 25 characters")
        String lastName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "\\d{9}", message = "Phone number must be exactly 9 digits")
        String phoneNumber
) {
}
