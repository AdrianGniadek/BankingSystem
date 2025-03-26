package com.adriangniadek.BankingSystem.service;

import com.adriangniadek.BankingSystem.dto.UserDTO;
import com.adriangniadek.BankingSystem.model.User;
import java.util.List;
import java.util.Optional;

public interface UserService {
    UserDTO createUser(UserDTO userDTO, String password);
    Optional<User> findByEmail(String email);
    List<UserDTO> getAllUsers();
    UserDTO updateUser(Long id, UserDTO userDTO);
    void deleteUser(Long id);
}
