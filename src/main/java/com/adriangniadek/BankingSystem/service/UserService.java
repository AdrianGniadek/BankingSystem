package com.adriangniadek.BankingSystem.service;

import com.adriangniadek.BankingSystem.dto.ChangePasswordRequest;
import com.adriangniadek.BankingSystem.dto.PageResponse;
import com.adriangniadek.BankingSystem.dto.RegisterRequest;
import com.adriangniadek.BankingSystem.dto.UpdateUserProfileRequest;
import com.adriangniadek.BankingSystem.dto.UpdateUserRequest;
import com.adriangniadek.BankingSystem.dto.UserDTO;
import com.adriangniadek.BankingSystem.dto.UserProfileDTO;

public interface UserService {
    UserDTO createUser(RegisterRequest request);
    PageResponse<UserDTO> getAllUsers(int page, int size);
    UserDTO updateUser(Long id, UpdateUserRequest request);
    void deleteUser(Long id);
    UserProfileDTO getUserProfile(String email);
    UserProfileDTO updateUserProfile(String email, UpdateUserProfileRequest request);
    void changePassword(String email, ChangePasswordRequest request);
}
