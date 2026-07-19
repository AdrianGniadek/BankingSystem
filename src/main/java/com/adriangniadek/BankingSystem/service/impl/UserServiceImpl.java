package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.ChangePasswordRequest;
import com.adriangniadek.BankingSystem.dto.PageResponse;
import com.adriangniadek.BankingSystem.dto.RegisterRequest;
import com.adriangniadek.BankingSystem.dto.UpdateUserProfileRequest;
import com.adriangniadek.BankingSystem.dto.UpdateUserRequest;
import com.adriangniadek.BankingSystem.dto.UserDTO;
import com.adriangniadek.BankingSystem.dto.UserProfileDTO;
import com.adriangniadek.BankingSystem.enums.RoleType;
import com.adriangniadek.BankingSystem.exception.BusinessRuleViolationException;
import com.adriangniadek.BankingSystem.exception.ResourceConflictException;
import com.adriangniadek.BankingSystem.exception.ResourceNotFoundException;
import com.adriangniadek.BankingSystem.mapper.UserMapper;
import com.adriangniadek.BankingSystem.model.Role;
import com.adriangniadek.BankingSystem.model.User;
import com.adriangniadek.BankingSystem.repository.RoleRepository;
import com.adriangniadek.BankingSystem.repository.UserRepository;
import com.adriangniadek.BankingSystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDTO createUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResourceConflictException("Email is already registered");
        }
        if (userRepository.existsByPesel(request.pesel())) {
            throw new ResourceConflictException("PESEL is already registered");
        }

        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("Required USER role is not configured"));

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPesel(request.pesel());
        user.setPhoneNumber(request.phoneNumber());
        user.setRoles(Set.of(userRole));

        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDTO> getAllUsers(int page, int size) {
        return PageResponse.from(userRepository.findAll(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(userMapper::toDto));
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        User user = getUserById(id);

        if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ResourceConflictException("Email already in use");
        }

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());

        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userRepository.delete(getUserById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(String email) {
        return userMapper.toProfileDto(getUserByEmail(email));
    }

    @Override
    @Transactional
    public UserProfileDTO updateUserProfile(String email, UpdateUserProfileRequest request) {
        User user = getUserByEmail(email);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhoneNumber(request.phoneNumber());

        return userMapper.toProfileDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getUserByEmail(email);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessRuleViolationException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessRuleViolationException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
