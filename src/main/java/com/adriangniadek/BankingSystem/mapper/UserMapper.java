package com.adriangniadek.BankingSystem.mapper;

import com.adriangniadek.BankingSystem.dto.UserDTO;
import com.adriangniadek.BankingSystem.model.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDTO toDto(User user) {
        return new UserDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet())
        );
    }

    public User toEntity(UserDTO userDTO) {
        User user = new User();
        user.setId(userDTO.id());
        user.setFirstName(userDTO.firstName());
        user.setLastName(userDTO.lastName());
        user.setEmail(userDTO.email());

        return user;
    }
}