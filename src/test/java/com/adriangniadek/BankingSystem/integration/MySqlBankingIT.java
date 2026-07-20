package com.adriangniadek.BankingSystem.integration;

import com.adriangniadek.BankingSystem.dto.CreateTransferRequest;
import com.adriangniadek.BankingSystem.enums.AccountStatus;
import com.adriangniadek.BankingSystem.enums.AccountType;
import com.adriangniadek.BankingSystem.exception.BusinessRuleViolationException;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.User;
import com.adriangniadek.BankingSystem.repository.AccountEntryRepository;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.TransferRepository;
import com.adriangniadek.BankingSystem.repository.UserRepository;
import com.adriangniadek.BankingSystem.service.AccountService;
import com.adriangniadek.BankingSystem.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class MySqlBankingIT {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private AccountEntryRepository accountEntryRepository;

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountService accountService;

    @BeforeEach
    void cleanBusinessData() {
        accountEntryRepository.deleteAllInBatch();
        transferRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void shouldApplyAllFlywayMigrationsToMySql() throws SQLException {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
        }

        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank",
                String.class);
        Integer roleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM roles", Integer.class);
        Integer ledgerTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = 'account_entries'",
                Integer.class);

        assertThat(versions).containsExactly("1", "2", "3");
        assertThat(roleCount).isEqualTo(2);
        assertThat(ledgerTableCount).isEqualTo(1);
    }

    @RepeatedTest(5)
    void shouldPreventNegativeBalanceDuringConcurrentTransfers() throws Exception {
        User owner = userRepository.saveAndFlush(user());
        Account source = accountRepository.saveAndFlush(account(owner, "10000000000000000001", "100.00"));
        Account firstTarget = accountRepository.saveAndFlush(account(owner, "10000000000000000002", "0.00"));
        Account secondTarget = accountRepository.saveAndFlush(account(owner, "10000000000000000003", "0.00"));

        CreateTransferRequest firstRequest = transferRequest(source.getId(), firstTarget.getId());
        CreateTransferRequest secondRequest = transferRequest(source.getId(), secondTarget.getId());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<OperationAttempt> first = executor.submit(
                    () -> executeTransferAsAdmin(firstRequest, ready, start));
            Future<OperationAttempt> second = executor.submit(
                    () -> executeTransferAsAdmin(secondRequest, ready, start));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<OperationAttempt> attempts = List.of(
                    first.get(15, TimeUnit.SECONDS),
                    second.get(15, TimeUnit.SECONDS));

            assertThat(attempts).filteredOn(OperationAttempt::successful).hasSize(1);
            assertThat(attempts).filteredOn(attempt -> !attempt.successful()).singleElement()
                    .satisfies(attempt -> assertThat(attempt.failure())
                            .isInstanceOf(BusinessRuleViolationException.class)
                            .hasMessage("Insufficient funds in source account"));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        Account updatedSource = accountRepository.findById(source.getId()).orElseThrow();
        Account updatedFirstTarget = accountRepository.findById(firstTarget.getId()).orElseThrow();
        Account updatedSecondTarget = accountRepository.findById(secondTarget.getId()).orElseThrow();

        assertThat(updatedSource.getBalance()).isEqualByComparingTo("20.00");
        assertThat(updatedFirstTarget.getBalance().add(updatedSecondTarget.getBalance()))
                .isEqualByComparingTo("80.00");
        assertThat(transferRepository.count()).isEqualTo(1);
        assertThat(accountEntryRepository.count()).isEqualTo(2);
    }

    @RepeatedTest(5)
    void shouldKeepClosedAccountEmptyWhenClosureRacesWithIncomingTransfer() throws Exception {
        User owner = userRepository.saveAndFlush(user());
        Account closingAccount = accountRepository.saveAndFlush(
                account(owner, "20000000000000000001", "0.00"));
        Account fundingAccount = accountRepository.saveAndFlush(
                account(owner, "20000000000000000002", "100.00"));
        CreateTransferRequest request = new CreateTransferRequest(
                UUID.randomUUID(),
                fundingAccount.getId(),
                closingAccount.getId(),
                new BigDecimal("10.00"),
                "PLN",
                "Concurrent closure transfer");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<OperationAttempt> transfer = executor.submit(
                    () -> executeAsAdmin(
                            () -> transferService.createTransfer(
                                    request, "integration-admin@example.com"),
                            ready,
                            start));
            Future<OperationAttempt> closure = executor.submit(
                    () -> executeAsAdmin(
                            () -> accountService.closeAccount(closingAccount.getId()),
                            ready,
                            start));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<OperationAttempt> attempts = List.of(
                    transfer.get(15, TimeUnit.SECONDS),
                    closure.get(15, TimeUnit.SECONDS));

            assertThat(attempts).filteredOn(OperationAttempt::successful).hasSize(1);
            assertThat(attempts).filteredOn(attempt -> !attempt.successful()).singleElement()
                    .satisfies(attempt -> {
                        assertThat(attempt.failure())
                                .isInstanceOf(BusinessRuleViolationException.class);
                        assertThat(attempt.failure().getMessage()).isIn(
                                "Target account must be active",
                                "Account balance must be zero before closing");
                    });
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        Account updatedAccount = accountRepository.findById(closingAccount.getId()).orElseThrow();
        if (updatedAccount.getStatus() == AccountStatus.CLOSED) {
            assertThat(updatedAccount.getBalance()).isEqualByComparingTo("0.00");
            assertThat(transferRepository.count()).isZero();
            assertThat(accountEntryRepository.count()).isZero();
        } else {
            assertThat(updatedAccount.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(updatedAccount.getBalance()).isEqualByComparingTo("10.00");
            assertThat(transferRepository.count()).isOne();
            assertThat(accountEntryRepository.count()).isEqualTo(2);
        }
    }

    private OperationAttempt executeTransferAsAdmin(
            CreateTransferRequest request,
            CountDownLatch ready,
            CountDownLatch start) throws InterruptedException {
        return executeAsAdmin(
                () -> transferService.createTransfer(request, "integration-admin@example.com"),
                ready,
                start);
    }

    private OperationAttempt executeAsAdmin(
            Runnable operation,
            CountDownLatch ready,
            CountDownLatch start) throws InterruptedException {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                "integration-admin@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        SecurityContextHolder.setContext(securityContext);
        ready.countDown();

        try {
            if (!start.await(10, TimeUnit.SECONDS)) {
                return OperationAttempt.failed(
                        new IllegalStateException("Concurrent operation start timed out"));
            }
            operation.run();
            return OperationAttempt.succeeded();
        } catch (RuntimeException exception) {
            return OperationAttempt.failed(exception);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private User user() {
        User user = new User();
        user.setFirstName("Integration");
        user.setLastName("Owner");
        user.setEmail("integration-owner@example.com");
        user.setPassword("encoded-password");
        user.setPhoneNumber("123456789");
        user.setPesel("12345678901");
        return user;
    }

    private Account account(User owner, String number, String balance) {
        Account account = new Account();
        account.setAccountNumber(number);
        account.setAccountType(AccountType.CHECKING);
        account.setBalance(new BigDecimal(balance));
        account.setCurrency("PLN");
        account.setUser(owner);
        return account;
    }

    private CreateTransferRequest transferRequest(Long sourceAccountId, Long targetAccountId) {
        return new CreateTransferRequest(
                UUID.randomUUID(),
                sourceAccountId,
                targetAccountId,
                new BigDecimal("80.00"),
                "PLN",
                "Concurrent integration transfer");
    }

    private record OperationAttempt(boolean successful, RuntimeException failure) {
        static OperationAttempt succeeded() {
            return new OperationAttempt(true, null);
        }

        static OperationAttempt failed(RuntimeException failure) {
            return new OperationAttempt(false, failure);
        }
    }
}
