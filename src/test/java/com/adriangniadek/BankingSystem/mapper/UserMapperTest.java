package com.adriangniadek.BankingSystem.mapper;

import com.adriangniadek.BankingSystem.dto.UserDTO;
import com.adriangniadek.BankingSystem.model.Role;
import com.adriangniadek.BankingSystem.enums.RoleType;
import com.adriangniadek.BankingSystem.model.User;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void testToDto() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setRoles(Set.of(new Role(1L, RoleType.ROLE_USER)));

        UserDTO userDTO = userMapper.toDto(user);

        assertNotNull(userDTO);
        assertEquals(user.getId(), userDTO.id());
        assertEquals(user.getFirstName(), userDTO.firstName());
        assertEquals(user.getLastName(), userDTO.lastName());
        assertEquals(user.getEmail(), userDTO.email());
        assertTrue(userDTO.roles().contains("ROLE_USER"));
    }

    @Test
    void testToEntity() {
        UserDTO userDTO = new UserDTO(1L, "John", "Doe", "john.doe@example.com", Set.of("ROLE_USER"));
        User user = userMapper.toEntity(userDTO);

        assertNotNull(user);
        assertEquals(userDTO.id(), user.getId());
        assertEquals(userDTO.firstName(), user.getFirstName());
        assertEquals(userDTO.lastName(), user.getLastName());
        assertEquals(userDTO.email(), user.getEmail());
    }
}