package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.ChangePasswordRequest;
import com.adriangniadek.BankingSystem.dto.UpdateUserProfileRequest;
import com.adriangniadek.BankingSystem.dto.UserProfileDTO;
import com.adriangniadek.BankingSystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@Validated
public class UserProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserProfileDTO> getUserProfile(Authentication authentication) {
        UserProfileDTO userProfile = userService.getUserProfile(authentication.getName());
        return ResponseEntity.ok(userProfile);
    }

    @PutMapping
    public ResponseEntity<UserProfileDTO> updateUserProfile(
            @Valid @RequestBody UpdateUserProfileRequest request,
            Authentication authentication) {
        UserProfileDTO updatedProfile = userService.updateUserProfile(authentication.getName(), request);
        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        userService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
