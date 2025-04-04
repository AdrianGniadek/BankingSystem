package com.adriangniadek.BankingSystem.service.impl;

import com.adriangniadek.BankingSystem.dto.UserDTO;
import com.adriangniadek.BankingSystem.enums.RoleType;
import com.adriangniadek.BankingSystem.model.Role;
import com.adriangniadek.BankingSystem.model.User;
import com.adriangniadek.BankingSystem.repository.RoleRepository;
import com.adriangniadek.BankingSystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserDTO userDTO;
    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role(1L, RoleType.ROLE_USER);
        user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setPassword("hashedPassword");
        user.setRoles(Set.of(role));
        userDTO = new UserDTO(1L, "John", "Doe", "john@example.com", Set.of("ROLE_USER"));
    }

    @Test
    void shouldCreateUserSuccessfully() {
        when(userRepository.existsByEmail(userDTO.email())).thenReturn(false);
        when(roleRepository.findByName(RoleType.ROLE_USER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDTO createdUser = userService.createUser(userDTO, "password");

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.email()).isEqualTo(userDTO.email());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail(userDTO.email())).thenReturn(true);

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                userService.createUser(userDTO, "password"));

        assertThat(exception.getMessage()).isEqualTo("Email already in use.");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldFindUserByEmail() {
        when(userRepository.findByEmail(userDTO.email())).thenReturn(Optional.of(user));

        Optional<User> foundUser = userService.findByEmail(userDTO.email());

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(userDTO.email());
    }

    @Test
    void shouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserDTO> users = userService.getAllUsers();

        assertThat(users).hasSize(1);
        assertThat(users.getFirst().email()).isEqualTo(userDTO.email());
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDTO updatedUser = userService.updateUser(user.getId(), userDTO);

        assertThat(updatedUser.email()).isEqualTo(userDTO.email());
        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentUser() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () ->
                userService.updateUser(2L, userDTO));

        assertThat(exception.getMessage()).isEqualTo("User not found");
    }

    @Test
    void shouldDeleteUserSuccessfully() {
        when(userRepository.existsById(user.getId())).thenReturn(true);

        userService.deleteUser(user.getId());

        verify(userRepository).deleteById(user.getId());
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentUser() {
        when(userRepository.existsById(2L)).thenReturn(false);

        Exception exception = assertThrows(RuntimeException.class, () ->
                userService.deleteUser(2L));

        assertThat(exception.getMessage()).isEqualTo("User not found");
        verify(userRepository, never()).deleteById(anyLong());
    }
}
