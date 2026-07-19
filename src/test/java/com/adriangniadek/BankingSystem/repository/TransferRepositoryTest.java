package com.adriangniadek.BankingSystem.repository;

import com.adriangniadek.BankingSystem.enums.RoleType;
import com.adriangniadek.BankingSystem.enums.AccountType;
import com.adriangniadek.BankingSystem.enums.TransferStatus;
import com.adriangniadek.BankingSystem.model.Account;
import com.adriangniadek.BankingSystem.model.Role;
import com.adriangniadek.BankingSystem.model.Transfer;
import com.adriangniadek.BankingSystem.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TransferRepositoryTest {

    @Autowired
    private TransferRepository transferRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldFindTransferForSourceAndTargetAccounts() {
        Role userRole = roleRepository.findByName(RoleType.ROLE_USER).orElseThrow();

        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword("password123");
        user.setPhoneNumber("123456789");
        user.setPesel("90010112345");
        user.addRole(userRole);
        user = userRepository.save(user);

        Account sourceAccount = new Account();
        sourceAccount.setAccountNumber("PL123456789");
        sourceAccount.setAccountType(AccountType.SAVINGS);
        sourceAccount.setBalance(BigDecimal.valueOf(1000));
        sourceAccount.setCurrency("PLN");
        sourceAccount.setUser(user);
        sourceAccount = accountRepository.save(sourceAccount);

        Account targetAccount = new Account();
        targetAccount.setAccountNumber("PL987654321");
        targetAccount.setAccountType(AccountType.CHECKING);
        targetAccount.setBalance(BigDecimal.valueOf(500));
        targetAccount.setCurrency("PLN");
        targetAccount.setUser(user);
        targetAccount = accountRepository.save(targetAccount);

        Transfer transfer = new Transfer();
        transfer.setSourceAccount(sourceAccount);
        transfer.setTargetAccount(targetAccount);
        transfer.setAmount(BigDecimal.valueOf(250));
        transfer.setCurrency("PLN");
        transfer.setDescription("Test transfer");
        transfer.setStatus(TransferStatus.COMPLETED);
        LocalDateTime transferTime = LocalDateTime.now();
        transfer.setCreatedAt(transferTime);

        transferRepository.saveAndFlush(transfer);

        Transfer failedTransfer = new Transfer();
        failedTransfer.setSourceAccount(sourceAccount);
        failedTransfer.setTargetAccount(targetAccount);
        failedTransfer.setAmount(BigDecimal.TEN);
        failedTransfer.setCurrency("PLN");
        failedTransfer.setDescription("Failed transfer");
        failedTransfer.setStatus(TransferStatus.FAILED);
        failedTransfer.setCreatedAt(transferTime.plusSeconds(1));
        transferRepository.saveAndFlush(failedTransfer);

        List<Transfer> sourceHistory = transferRepository
                .findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(
                        sourceAccount.getId(), sourceAccount.getId());
        List<Transfer> targetHistory = transferRepository
                .findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(
                        targetAccount.getId(), targetAccount.getId());
        List<Transfer> completedTransfers = transferRepository.findByAccountIdFromDate(
                sourceAccount.getId(), transferTime.minusSeconds(1), TransferStatus.COMPLETED);

        assertThat(sourceHistory).hasSize(2);
        assertThat(targetHistory).hasSize(2);
        assertThat(sourceHistory).extracting(Transfer::getStatus)
                .containsExactly(TransferStatus.FAILED, TransferStatus.COMPLETED);
        assertThat(completedTransfers).hasSize(1);
        assertThat(completedTransfers.getFirst().getAmount()).isEqualByComparingTo("250.00");
    }
}
