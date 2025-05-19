package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.dto.UserDTO;
import com.adriangniadek.BankingSystem.repository.AccountRepository;
import com.adriangniadek.BankingSystem.repository.TransferRepository;
import com.adriangniadek.BankingSystem.repository.UserRepository;
import com.adriangniadek.BankingSystem.service.TransferService;
import com.adriangniadek.BankingSystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final UserService userService;
    private final TransferService transferService;

    @GetMapping("/")
    public String home(Model model) {
        long userCount = userRepository.count();
        long accountCount = accountRepository.count();
        long transferCount = transferRepository.count();

        List<UserDTO> recentUsers = userService.getAllUsers().stream()
                .limit(5)
                .toList();

        List<Object> recentTransfers = transferService.getAllTransfers().stream()
                .limit(5)
                .toList();

        model.addAttribute("userCount", userCount);
        model.addAttribute("accountCount", accountCount);
        model.addAttribute("transferCount", transferCount);
        model.addAttribute("recentUsers", recentUsers);
        model.addAttribute("recentTransfers", recentTransfers);

        return "index";
    }
}