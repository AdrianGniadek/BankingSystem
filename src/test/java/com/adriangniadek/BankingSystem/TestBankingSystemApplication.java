package com.adriangniadek.BankingSystem;

import org.springframework.boot.SpringApplication;

public class TestBankingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.from(BankingSystemApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
