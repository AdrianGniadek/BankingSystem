package com.adriangniadek.BankingSystem.repository;

import com.adriangniadek.BankingSystem.enums.RoleType;
import com.adriangniadek.BankingSystem.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleType name);
}
