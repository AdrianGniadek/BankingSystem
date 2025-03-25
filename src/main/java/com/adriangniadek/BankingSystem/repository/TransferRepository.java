package com.adriangniadek.BankingSystem.repository;

import com.adriangniadek.BankingSystem.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    List<Transfer> findBySourceAccountId(Long accountId);
    List<Transfer> findByTargetAccountId(Long accountId);
}
