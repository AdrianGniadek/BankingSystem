package com.adriangniadek.BankingSystem.repository;

import com.adriangniadek.BankingSystem.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    List<Transfer> findBySourceAccountId(Long accountId);
    List<Transfer> findByTargetAccountId(Long accountId);
    
    @Query("SELECT t FROM Transfer t WHERE (t.sourceAccount.id = :accountId OR t.targetAccount.id = :accountId) " +
           "AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt")
    List<Transfer> findByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
