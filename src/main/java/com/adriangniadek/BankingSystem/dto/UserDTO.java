package com.adriangniadek.BankingSystem.dto;

import jakarta.validation.constraints.*;
import java.util.Set;

public record UserDTO(Long id,
                      @NotBlank(message = "First name is required")
                      @Size(min = 2, max = 25, message = "First name must be between 2 and 25 characters")
                      String firstName,

                      @NotBlank(message = "Last name is required")
                      @Size(min = 2, max = 25, message = "Last name must be between 2 and 25 characters")
                      String lastName,

                      @NotBlank(message = "Email is required")
                      @Email(message = "Email should be valid")
                      String email,

                      Set<String> roles) {
}
