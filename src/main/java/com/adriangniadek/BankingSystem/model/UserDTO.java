package com.adriangniadek.BankingSystem.model;

import java.util.Set;

public record UserDTO(Long id,
                      String firstName,
                      String lastName,
                      String email,
                      Set<String> roles) {
}
