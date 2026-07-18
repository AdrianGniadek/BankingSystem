package com.adriangniadek.BankingSystem.dto;

public record UserProfileDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber
) {
}
