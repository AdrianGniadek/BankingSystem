package com.adriangniadek.BankingSystem.integration;

import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.dto.AccountEntryDTO;
import com.adriangniadek.BankingSystem.dto.AccountStatementDTO;
import com.adriangniadek.BankingSystem.dto.CreateAccountRequest;
import com.adriangniadek.BankingSystem.dto.CreateDemoDepositRequest;
import com.adriangniadek.BankingSystem.dto.CreateTransferRequest;
import com.adriangniadek.BankingSystem.dto.LoginRequest;
import com.adriangniadek.BankingSystem.dto.RegisterRequest;
import com.adriangniadek.BankingSystem.dto.TokenResponse;
import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.dto.UpdateUserProfileRequest;
import com.adriangniadek.BankingSystem.dto.UserProfileDTO;
import com.adriangniadek.BankingSystem.enums.AccountEntryType;
import com.adriangniadek.BankingSystem.enums.AccountType;
import com.adriangniadek.BankingSystem.enums.TransferStatus;
import com.adriangniadek.BankingSystem.repository.AccountEntryRepository;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.RefreshTokenRepository;
import com.adriangniadek.BankingSystem.repository.TransferRepository;
import com.adriangniadek.BankingSystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CoreUserWorkflowIT {

    private static final String EMAIL = "workflow@example.com";
    private static final String PASSWORD = "password123";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private AccountEntryRepository accountEntryRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanBusinessData() {
        refreshTokenRepository.deleteAllInBatch();
        accountEntryRepository.deleteAllInBatch();
        transferRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void shouldCompleteCoreUserWorkflow() {
        registerUser();
        ResponseEntity<TokenResponse> loginResponse = login();
        String accessToken = loginResponse.getBody().accessToken();
        String refreshCookie = cookieFrom(loginResponse);

        AccountDTO checkingAccount = createAccount(accessToken, AccountType.CHECKING);
        AccountDTO savingsAccount = createAccount(accessToken, AccountType.SAVINGS);

        ResponseEntity<AccountEntryDTO> depositResponse = authorizedExchange(
                HttpMethod.POST,
                "/demo/accounts/" + checkingAccount.id() + "/deposits",
                new CreateDemoDepositRequest(UUID.randomUUID(), new BigDecimal("1000.00"), "PLN"),
                AccountEntryDTO.class,
                accessToken);
        assertThat(depositResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(depositResponse.getBody().type()).isEqualTo(AccountEntryType.DEPOSIT);

        ResponseEntity<TransferDTO> transferResponse = authorizedExchange(
                HttpMethod.POST,
                "/transfers",
                new CreateTransferRequest(
                        UUID.randomUUID(),
                        checkingAccount.id(),
                        savingsAccount.accountNumber(),
                        new BigDecimal("250.00"),
                        "PLN",
                        "Monthly savings"),
                TransferDTO.class,
                accessToken);
        assertThat(transferResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(transferResponse.getBody().status()).isEqualTo(TransferStatus.COMPLETED);

        ResponseEntity<AccountDTO[]> accountsResponse = authorizedExchange(
                HttpMethod.GET, "/accounts", null, AccountDTO[].class, accessToken);
        assertThat(accountsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Arrays.asList(accountsResponse.getBody()))
                .filteredOn(account -> account.id().equals(checkingAccount.id()))
                .singleElement()
                .extracting(AccountDTO::balance)
                .satisfies(balance -> assertThat(balance).isEqualByComparingTo("750.00"));
        assertThat(Arrays.asList(accountsResponse.getBody()))
                .filteredOn(account -> account.id().equals(savingsAccount.id()))
                .singleElement()
                .extracting(AccountDTO::balance)
                .satisfies(balance -> assertThat(balance).isEqualByComparingTo("250.00"));

        String statementPath = UriComponentsBuilder
                .fromPath("/accounts/{accountId}/statement")
                .queryParam("startDate", LocalDateTime.now().minusDays(1))
                .queryParam("endDate", LocalDateTime.now().plusDays(1))
                .buildAndExpand(checkingAccount.id())
                .encode()
                .toUriString();
        ResponseEntity<AccountStatementDTO> statementResponse = authorizedExchange(
                HttpMethod.GET, statementPath, null, AccountStatementDTO.class, accessToken);
        assertThat(statementResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statementResponse.getBody().closingBalance()).isEqualByComparingTo("750.00");
        assertThat(statementResponse.getBody().entries())
                .extracting(AccountEntryDTO::type)
                .containsExactly(AccountEntryType.DEPOSIT, AccountEntryType.TRANSFER_OUT);

        ResponseEntity<UserProfileDTO> profileResponse = authorizedExchange(
                HttpMethod.PUT,
                "/profile",
                new UpdateUserProfileRequest("Jan", "Nowak", "987654321"),
                UserProfileDTO.class,
                accessToken);
        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profileResponse.getBody())
                .extracting(
                        UserProfileDTO::firstName,
                        UserProfileDTO::lastName,
                        UserProfileDTO::phoneNumber)
                .containsExactly("Jan", "Nowak", "987654321");

        HttpHeaders logoutHeaders = new HttpHeaders();
        logoutHeaders.add(HttpHeaders.COOKIE, refreshCookie);
        ResponseEntity<Void> logoutResponse = restTemplate.exchange(
                "/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(logoutHeaders),
                Void.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<TokenResponse> refreshResponse = restTemplate.exchange(
                "/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(logoutHeaders),
                TokenResponse.class);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void registerUser() {
        RegisterRequest request = new RegisterRequest(
                "Anna",
                "Kowalska",
                EMAIL,
                PASSWORD,
                "90010112345",
                "123456789");

        ResponseEntity<Void> response = restTemplate.postForEntity("/auth/register", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private ResponseEntity<TokenResponse> login() {
        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                "/auth/login",
                new LoginRequest(EMAIL, PASSWORD),
                TokenResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().tokenType()).isEqualTo("Bearer");
        return response;
    }

    private AccountDTO createAccount(String accessToken, AccountType accountType) {
        ResponseEntity<AccountDTO> response = authorizedExchange(
                HttpMethod.POST,
                "/accounts",
                new CreateAccountRequest(accountType, "PLN"),
                AccountDTO.class,
                accessToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private String cookieFrom(ResponseEntity<?> response) {
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotBlank();
        return setCookie.substring(0, setCookie.indexOf(';'));
    }

    private <T> ResponseEntity<T> authorizedExchange(
            HttpMethod method,
            String path,
            Object body,
            Class<T> responseType,
            String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), responseType);
    }
}
