package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.ChangePasswordRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private Role role;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        role = new Role(1L, RoleType.ROLE_USER);
        user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setPassword("hashedPassword");
        user.setPesel("90010112345");
        user.setPhoneNumber("123456789");
        user.setRoles(Set.of(role));

        registerRequest = new RegisterRequest(
                "John",
                "Doe",
                "john@example.com",
                "password123",
                "90010112345",
                "123456789"
        );
    }

    @Test
    void shouldCreateUserSuccessfully() {
        when(roleRepository.findByName(RoleType.ROLE_USER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDTO createdUser = userService.createUser(registerRequest);

        assertThat(createdUser.email()).isEqualTo(registerRequest.email());
        verify(userRepository).existsByEmail(registerRequest.email());
        verify(userRepository).existsByPesel(registerRequest.pesel());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldRejectRegisteredEmail() {
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(registerRequest))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Email is already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectRegisteredPesel() {
        when(userRepository.existsByPesel(registerRequest.pesel())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(registerRequest))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("PESEL is already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldReturnAllUsers() {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));

        var users = userService.getAllUsers(0, 20);

        assertThat(users.content()).singleElement().extracting(UserDTO::email).isEqualTo(user.getEmail());
        assertThat(users.totalElements()).isEqualTo(1);
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        UpdateUserRequest request = new UpdateUserRequest("Jane", "Doe", "jane@example.com");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserDTO updatedUser = userService.updateUser(user.getId(), request);

        assertThat(updatedUser.firstName()).isEqualTo("Jane");
        assertThat(updatedUser.email()).isEqualTo("jane@example.com");
        verify(userRepository).existsByEmailAndIdNot(request.email(), user.getId());
    }

    @Test
    void shouldRejectEmailUsedByAnotherUser() {
        UpdateUserRequest request = new UpdateUserRequest("Jane", "Doe", "used@example.com");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot(request.email(), user.getId())).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(user.getId(), request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Email already in use");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdateOfMissingUser() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(
                2L, new UpdateUserRequest("Jane", "Doe", "jane@example.com")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void shouldDeleteExistingUser() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService.deleteUser(user.getId());

        verify(userRepository).delete(user);
    }

    @Test
    void shouldRejectDeletionOfMissingUser() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void shouldReturnUserProfile() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        UserProfileDTO profile = userService.getUserProfile(user.getEmail());

        assertThat(profile.email()).isEqualTo(user.getEmail());
        assertThat(profile.phoneNumber()).isEqualTo(user.getPhoneNumber());
    }

    @Test
    void shouldUpdateEditableProfileFields() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("Jane", "Smith", "987654321");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserProfileDTO profile = userService.updateUserProfile(user.getEmail(), request);

        assertThat(profile.firstName()).isEqualTo("Jane");
        assertThat(profile.phoneNumber()).isEqualTo("987654321");
        assertThat(profile.email()).isEqualTo("john@example.com");
    }

    @Test
    void shouldChangePassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("password123", "newPassword123");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.currentPassword(), user.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(request.newPassword(), user.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(request.newPassword())).thenReturn("newHash");

        userService.changePassword(user.getEmail(), request);

        assertThat(user.getPassword()).isEqualTo("newHash");
        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectIncorrectCurrentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("wrongPassword", "newPassword123");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.changePassword(user.getEmail(), request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Current password is incorrect");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectCurrentPasswordAsNewPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("password123", "password123");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.currentPassword(), user.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(user.getEmail(), request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("New password must be different from current password");

        verify(userRepository, never()).save(any());
    }
}
