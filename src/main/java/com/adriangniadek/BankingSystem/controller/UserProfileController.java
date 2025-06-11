package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.UserProfileDTO;
import com.adriangniadek.BankingSystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
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
    public ResponseEntity<?> changePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword) {

        String email = getCurrentUserEmail();
        userService.changePassword(email, currentPassword, newPassword);
        return ResponseEntity.ok().build();
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
