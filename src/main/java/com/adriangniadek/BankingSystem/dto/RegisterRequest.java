package com.adriangniadek.BankingSystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 30, message = "First name must be between 2 and 30 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 30, message = "Last name must be between 2 and 30 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters long")
        String password,

        @NotBlank(message = "PESEL is required")
        @Pattern(regexp = "\\d{11}", message = "PESEL must be exactly 11 digits")
        String pesel,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "\\d{9}", message = "Phone number must be exactly 9 digits")
        String phoneNumber
) {}
