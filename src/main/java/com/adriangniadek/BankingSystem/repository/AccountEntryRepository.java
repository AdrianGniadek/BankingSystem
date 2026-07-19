package com.adriangniadek.BankingSystem.repository;

import com.adriangniadek.BankingSystem.model.AccountEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountEntryRepository extends JpaRepository<AccountEntry, Long> {

    Optional<AccountEntry> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT e FROM AccountEntry e WHERE e.account.id = :accountId " +
            "AND e.createdAt >= :startDate ORDER BY e.createdAt")
    List<AccountEntry> findByAccountIdFromDate(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDateTime startDate);
}
