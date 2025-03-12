package com.adriangniadek.BankingSystem.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Data
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private String currency;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
