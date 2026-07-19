package com.adriangniadek.BankingSystem.repository;

import com.adriangniadek.BankingSystem.model.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    Page<Transfer> findBySourceAccountIdOrTargetAccountId(
            Long sourceAccountId, Long targetAccountId, Pageable pageable);

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
}
