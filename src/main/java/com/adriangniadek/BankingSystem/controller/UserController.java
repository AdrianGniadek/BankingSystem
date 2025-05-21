package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.UserDTO;
import com.adriangniadek.BankingSystem.dto.AccountDTO;
import com.adriangniadek.BankingSystem.service.UserService;
import com.adriangniadek.BankingSystem.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private static final String USER_FORM_VIEW = "users/form";
    private final UserService userService;
    private final AccountService accountService;

    @GetMapping
    public String listUsers(Model model) {
        List<UserDTO> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "users/list";
    }

    @GetMapping("/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        UserDTO user = userService.getUserById(id);
        List<AccountDTO> accounts = accountService.getUserAccounts(id);

        model.addAttribute("user", user);
        model.addAttribute("accounts", accounts);

        return "users/view";
    }

    @GetMapping("/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new UserDTO(null, "", "", "", null));
        return USER_FORM_VIEW;
    }

    @PostMapping("/new")
    public String createUser(@Valid @ModelAttribute("user") UserDTO userDTO,
                             BindingResult result,
                             @RequestParam String password,
                             Model model) {
        if (result.hasErrors()) {
            return USER_FORM_VIEW;
        }

        try {
            UserDTO savedUser = userService.createUser(userDTO, password);
            return redirectToUserView(savedUser);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return USER_FORM_VIEW;
        }
    }

    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        UserDTO user = userService.getUserById(id);
        model.addAttribute("user", user);
        return USER_FORM_VIEW;
    }

    @PostMapping("/{id}/edit")
    public String updateUser(@PathVariable Long id,
                             @Valid @ModelAttribute("user") UserDTO userDTO,
                             BindingResult result) {

        if (result.hasErrors()) {
            return USER_FORM_VIEW;

        }

        UserDTO updatedUser = userService.updateUser(id, userDTO);
        return redirectToUserView(updatedUser);
    }

    private String redirectToUserView(UserDTO user) {
        return "redirect:/users/" + user.id();
    }

    @GetMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/users";
    }
}
