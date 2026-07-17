package com.adriangniadek.BankingSystem.security;

import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("bankingAuthorization")
@RequiredArgsConstructor
public class BankingAuthorization {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public boolean canAccessUser(Long userId, Authentication authentication) {
        return isAuthenticated(authentication)
                && userRepository.existsByIdAndEmail(userId, authentication.getName());
    }

    public boolean canAccessAccount(Long accountId, Authentication authentication) {
        return isAuthenticated(authentication)
                && accountRepository.existsByIdAndUserEmail(accountId, authentication.getName());
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }
}
