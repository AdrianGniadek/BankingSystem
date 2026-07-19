package com.adriangniadek.BankingSystem.repository;

import com.adriangniadek.BankingSystem.enums.TransferStatus;
import com.adriangniadek.BankingSystem.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    List<Transfer> findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(
            Long sourceAccountId, Long targetAccountId);
    
    @Query("SELECT t FROM Transfer t WHERE " +
            "(t.sourceAccount.id = :accountId OR t.targetAccount.id = :accountId) " +
            "AND t.status = :status AND t.createdAt >= :startDate ORDER BY t.createdAt")
    List<Transfer> findByAccountIdFromDate(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("status") TransferStatus status);
}
