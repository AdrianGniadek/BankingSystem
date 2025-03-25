package com.adriangniadek.BankingSystem.model;

import com.adriangniadek.BankingSystem.enums.RoleType;
import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private RoleType name;
}

