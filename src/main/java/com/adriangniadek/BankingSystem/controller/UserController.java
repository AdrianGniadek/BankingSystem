package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.PageResponse;
import com.adriangniadek.BankingSystem.dto.RegisterRequest;
import com.adriangniadek.BankingSystem.dto.UpdateUserRequest;
import com.adriangniadek.BankingSystem.dto.UserDTO;
import com.adriangniadek.BankingSystem.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody @Valid RegisterRequest request) {
        UserDTO savedUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0")
            @PositiveOrZero(message = "Page must not be negative") int page,
            @RequestParam(defaultValue = "20")
            @Positive(message = "Page size must be positive")
            @Max(value = 100, message = "Page size must not exceed 100") int size) {
        PageResponse<UserDTO> users = userService.getAllUsers(page, size);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable @Positive(message = "User ID must be positive") Long id,
            @RequestBody @Valid UpdateUserRequest request) {
        UserDTO updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable @Positive(message = "User ID must be positive") Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
