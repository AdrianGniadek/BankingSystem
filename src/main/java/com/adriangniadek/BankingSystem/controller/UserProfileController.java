package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.UserProfileDTO;
import com.adriangniadek.BankingSystem.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@Validated
public class UserProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserProfileDTO> getUserProfile() {
        String email = getCurrentUserEmail();
        UserProfileDTO userProfile = userService.getUserProfile(email);
        return ResponseEntity.ok(userProfile);
    }

    @PutMapping
    public ResponseEntity<UserProfileDTO> updateUserProfile(@Valid @RequestBody UserProfileDTO userProfileDTO) {
        String email = getCurrentUserEmail();
        UserProfileDTO updatedProfile = userService.updateUserProfile(email, userProfileDTO);
        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestParam("currentPassword")
            @NotBlank(message = "Current password is required") String currentPassword,
            @RequestParam("newPassword")
            @Size(min = 8, max = 72, message = "New password must be between 8 and 72 characters")
            String newPassword) {

        String email = getCurrentUserEmail();
        userService.changePassword(email, currentPassword, newPassword);
        return ResponseEntity.ok().build();
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
