package com.adriangniadek.BankingSystem.mapper;

import com.adriangniadek.BankingSystem.dto.UserDTO;
import com.adriangniadek.BankingSystem.dto.UserProfileDTO;
import com.adriangniadek.BankingSystem.enums.RoleType;
import com.adriangniadek.BankingSystem.model.Role;
import com.adriangniadek.BankingSystem.model.User;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void shouldMapUserToDto() {
        User user = createUser();

        UserDTO userDTO = userMapper.toDto(user);

        assertThat(userDTO.id()).isEqualTo(user.getId());
        assertThat(userDTO.email()).isEqualTo(user.getEmail());
        assertThat(userDTO.roles()).containsExactly("ROLE_USER");
    }

    @Test
    void shouldMapUserToProfileDto() {
        User user = createUser();

        UserProfileDTO profile = userMapper.toProfileDto(user);

        assertThat(profile.id()).isEqualTo(user.getId());
        assertThat(profile.firstName()).isEqualTo(user.getFirstName());
        assertThat(profile.phoneNumber()).isEqualTo(user.getPhoneNumber());
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setPhoneNumber("123456789");
        user.setRoles(Set.of(new Role(1L, RoleType.ROLE_USER)));
        return user;
    }
}
