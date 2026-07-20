package com.adriangniadek.BankingSystem.security;

import com.adriangniadek.BankingSystem.enums.AccountStatus;
import com.adriangniadek.BankingSystem.enums.AccountType;
import com.adriangniadek.BankingSystem.enums.RoleType;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.Role;
import com.adriangniadek.BankingSystem.model.User;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.AccountEntryRepository;
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
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private AccountEntryRepository accountEntryRepository;

    @Autowired
    private TransferRepository transferRepository;

    private Account ownerAccount;
    private Account otherAccount;
    private Long ownerUserId;
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        Role userRole = roleRepository.findByName(RoleType.ROLE_USER).orElseThrow();
        User owner = userRepository.save(user(
                "owner@example.com", "12345678901", "123456789", userRole));
        ownerUserId = owner.getId();
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
    void shouldCreateAccountWithServerGeneratedValues() throws Exception {
        String requestBody = """
                {
                  "accountType": "SAVINGS",
                  "currency": "PLN"
                }
                """;

        mockMvc.perform(post("/accounts/{userId}", ownerUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value(
                        org.hamcrest.Matchers.matchesPattern("[1-9][0-9]{19}")))
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"));
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void shouldReturnAccountsForCurrentUser() throws Exception {
        mockMvc.perform(get("/accounts/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(ownerAccount.getId()))
                .andExpect(jsonPath("$[0].userId").value(ownerUserId));
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void shouldCreateAccountForCurrentUser() throws Exception {
        String requestBody = """
                {
                  "accountType": "SAVINGS",
                  "currency": "EUR"
                }
                """;

        mockMvc.perform(post("/accounts/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(ownerUserId))
                .andExpect(jsonPath("$.currency").value("EUR"));

        assertThat(accountRepository.findByUserEmail("owner@example.com")).hasSize(2);
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
    void shouldRejectTransferEndpointForAnotherUsersSourceAccount() throws Exception {
        String requestBody = """
                {
                  "idempotencyKey": "%s",
                  "sourceAccountId": %d,
                  "targetAccountId": %d,
                  "amount": 10.00,
                  "currency": "PLN",
                  "description": "Unauthorized transfer"
                }
                """.formatted(UUID.randomUUID(), otherAccount.getId(), ownerAccount.getId());

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        assertThat(transferRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void shouldCreateTransferOnlyOnceWhenRequestIsRepeated() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        String requestBody = """
                {
                  "idempotencyKey": "%s",
                  "sourceAccountId": %d,
                  "targetAccountId": %d,
                  "amount": 25.00,
                  "currency": "PLN",
                  "description": "Authorized transfer"
                }
                """.formatted(idempotencyKey, ownerAccount.getId(), otherAccount.getId());

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.createdAt").exists());

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        assertThat(accountRepository.findById(ownerAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("75.00");
        assertThat(accountRepository.findById(otherAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("125.00");
        assertThat(transferRepository.count()).isOne();
        assertThat(accountEntryRepository.count()).isEqualTo(2);
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void shouldAllowAdministratorToDepositFunds() throws Exception {
        LocalDateTime statementStart = LocalDateTime.now().minusMinutes(1);
        String requestBody = """
                {
                  "idempotencyKey": "%s",
                  "amount": 50.00,
                  "currency": "PLN",
                  "description": "Cash deposit"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/accounts/{accountId}/deposits", ownerAccount.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(50.0));

        assertThat(accountRepository.findById(ownerAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("150.00");
        assertThat(accountEntryRepository.count()).isOne();

        mockMvc.perform(get("/accounts/statement/{accountId}", ownerAccount.getId())
                        .param("startDate", statementStart.toString())
                        .param("endDate", LocalDateTime.now().plusMinutes(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingBalance").value(100.0))
                .andExpect(jsonPath("$.closingBalance").value(150.0))
                .andExpect(jsonPath("$.entries[0].type").value("DEPOSIT"));
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void shouldRejectUserDeposit() throws Exception {
        String requestBody = """
                {
                  "idempotencyKey": "%s",
                  "amount": 50.00,
                  "currency": "PLN",
                  "description": "Unauthorized deposit"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/accounts/{accountId}/deposits", ownerAccount.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        assertThat(accountEntryRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void shouldAllowAdministratorToBlockAccount() throws Exception {
        mockMvc.perform(patch("/accounts/{accountId}/status", ownerAccount.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOCKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        assertThat(accountRepository.findById(ownerAccount.getId()).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.BLOCKED);
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void shouldRejectUserAccountStatusUpdate() throws Exception {
        mockMvc.perform(patch("/accounts/{accountId}/status", ownerAccount.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOCKED\"}"))
                .andExpect(status().isForbidden());

        assertThat(accountRepository.findById(ownerAccount.getId()).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void shouldAllowOwnerToCloseEmptyAccount() throws Exception {
        ownerAccount.setBalance(BigDecimal.ZERO.setScale(2));
        accountRepository.saveAndFlush(ownerAccount);

        mockMvc.perform(delete("/accounts/{accountId}", ownerAccount.getId()))
                .andExpect(status().isNoContent());

        assertThat(accountRepository.findById(ownerAccount.getId()).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void shouldRejectClosingAnotherUsersAccount() throws Exception {
        otherAccount.setBalance(BigDecimal.ZERO.setScale(2));
        accountRepository.saveAndFlush(otherAccount);

        mockMvc.perform(delete("/accounts/{accountId}", otherAccount.getId()))
                .andExpect(status().isForbidden());

        assertThat(accountRepository.findById(otherAccount.getId()).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.ACTIVE);
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
        account.setAccountType(AccountType.CHECKING);
        account.setBalance(new BigDecimal("100.00"));
        account.setCurrency("PLN");
        account.setStatus(AccountStatus.ACTIVE);
        account.setUser(owner);
        return account;
    }
}
