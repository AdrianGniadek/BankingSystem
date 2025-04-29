package com.adriangniadek.BankingSystem.repository;

import com.adriangniadek.BankingSystem.enums.RoleType;
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
    void shouldSaveAndFindTransferBySourceAccountId() {
        Role userRole = new Role();
        userRole.setName(RoleType.ROLE_USER);
        roleRepository.save(userRole);

        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword("password123");
        user.addRole(userRole);
        user = userRepository.save(user);

        Account sourceAccount = new Account();
        sourceAccount.setAccountNumber("PL123456789");
        sourceAccount.setAccountType("SAVINGS");
        sourceAccount.setBalance(BigDecimal.valueOf(1000));
        sourceAccount.setCurrency("PLN");
        sourceAccount.setUser(user);
        sourceAccount = accountRepository.save(sourceAccount);

        Account targetAccount = new Account();
        targetAccount.setAccountNumber("PL987654321");
        targetAccount.setAccountType("CHECKING");
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
        transfer.setStatus("COMPLETED");
        transfer.setCreatedAt(LocalDateTime.now());

        transferRepository.save(transfer);

        List<Transfer> result = transferRepository.findBySourceAccountId(sourceAccount.getId());
        
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getSourceAccount().getId()).isEqualTo(sourceAccount.getId());
        assertThat(result.get(0).getAmount()).isEqualTo(BigDecimal.valueOf(250));
    }
}
