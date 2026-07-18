package com.adriangniadek.BankingSystem.service;

import com.adriangniadek.BankingSystem.dto.ChangePasswordRequest;
import com.adriangniadek.BankingSystem.dto.RegisterRequest;
import com.adriangniadek.BankingSystem.dto.UpdateUserProfileRequest;
import com.adriangniadek.BankingSystem.dto.UpdateUserRequest;
import com.adriangniadek.BankingSystem.dto.UserDTO;
import com.adriangniadek.BankingSystem.dto.UserProfileDTO;

import java.util.List;

public interface UserService {
    UserDTO createUser(RegisterRequest request);
    List<UserDTO> getAllUsers();
    UserDTO updateUser(Long id, UpdateUserRequest request);
    void deleteUser(Long id);
    UserProfileDTO getUserProfile(String email);
    UserProfileDTO updateUserProfile(String email, UpdateUserProfileRequest request);
    void changePassword(String email, ChangePasswordRequest request);
}
