package com.adriangniadek.BankingSystem.security;

import com.adriangniadek.BankingSystem.enums.RoleType;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.Role;
import com.adriangniadek.BankingSystem.model.User;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.RoleRepository;
import com.adriangniadek.BankingSystem.repository.TransferRepository;
import com.adriangniadek.BankingSystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountAccessSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    private Account ownerAccount;
    private Account otherAccount;
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        Role userRole = roleRepository.findByName(RoleType.ROLE_USER).orElseThrow();
        User owner = userRepository.save(user(
                "owner@example.com", "12345678901", "123456789", userRole));
        User other = userRepository.save(user(
                "other@example.com", "10987654321", "987654321", userRole));
        otherUserId = other.getId();

        ownerAccount = accountRepository.save(account("11111111111111111111", owner));
        otherAccount = accountRepository.save(account("22222222222222222222", other));
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void shouldAllowOwnerToReadAccountBalance() throws Exception {
        mockMvc.perform(get("/accounts/balance/{accountId}", ownerAccount.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string("100.00"));
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void shouldRejectAccessToAnotherUsersAccount() throws Exception {
        mockMvc.perform(get("/accounts/balance/{accountId}", otherAccount.getId()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Access denied"));
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void shouldRejectAccessToAnotherUsersAccountList() throws Exception {
        mockMvc.perform(get("/accounts/{userId}", otherUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdministratorToReadAnyAccount() throws Exception {
        mockMvc.perform(get("/accounts/balance/{accountId}", otherAccount.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void shouldRejectTransferFromAnotherUsersAccount() throws Exception {
        mockMvc.perform(post("/accounts/transfer")
                        .param("sourceAccountId", otherAccount.getId().toString())
                        .param("targetAccountId", ownerAccount.getId().toString())
                        .param("amount", "10.00")
                        .param("currency", "PLN"))
                .andExpect(status().isForbidden());

        assertThat(accountRepository.findById(ownerAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("100.00");
        assertThat(accountRepository.findById(otherAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("100.00");
        assertThat(transferRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void shouldRejectTransferEndpointForAnotherUsersSourceAccount() throws Exception {
        String requestBody = """
                {
                  "sourceAccountId": %d,
                  "targetAccountId": %d,
                  "amount": 10.00,
                  "currency": "PLN",
                  "description": "Unauthorized transfer",
                  "status": "PENDING"
                }
                """.formatted(otherAccount.getId(), ownerAccount.getId());

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        assertThat(transferRepository.count()).isZero();
    }

    private User user(String email, String pesel, String phoneNumber, Role role) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("password123");
        user.setPesel(pesel);
        user.setPhoneNumber(phoneNumber);
        user.setRoles(Set.of(role));
        return user;
    }

    private Account account(String accountNumber, User owner) {
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setAccountType("CHECKING");
        account.setBalance(new BigDecimal("100.00"));
        account.setCurrency("PLN");
        account.setUser(owner);
        return account;
    }
}
