package com.adriangniadek.BankingSystem.controller;

import com.adriangniadek.BankingSystem.dto.ChangePasswordRequest;
import com.adriangniadek.BankingSystem.dto.UpdateUserProfileRequest;
import com.adriangniadek.BankingSystem.dto.UserProfileDTO;
import com.adriangniadek.BankingSystem.security.JwtAuthFilter;
import com.adriangniadek.BankingSystem.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {

    private static final String EMAIL = "john@example.com";
    private static final TestingAuthenticationToken AUTHENTICATION =
            new TestingAuthenticationToken(EMAIL, null);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void shouldReturnCurrentUserProfile() throws Exception {
        when(userService.getUserProfile(EMAIL))
                .thenReturn(new UserProfileDTO(1L, "John", "Doe", EMAIL, "123456789"));

        mockMvc.perform(get("/profile").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.phoneNumber").value("123456789"));
    }

    @Test
    void shouldUpdateEditableProfileFields() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("Jane", "Doe", "987654321");
        when(userService.updateUserProfile(EMAIL, request))
                .thenReturn(new UserProfileDTO(1L, "Jane", "Doe", EMAIL, "987654321"));

        mockMvc.perform(put("/profile")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void shouldChangePasswordFromJsonRequest() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("password123", "newPassword123");

        mockMvc.perform(post("/profile/change-password")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).changePassword(EMAIL, request);
    }

    @Test
    void shouldRejectInvalidProfileUpdate() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("J", "Doe", "123");

        mockMvc.perform(put("/profile")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.phoneNumber").exists());
    }
}
