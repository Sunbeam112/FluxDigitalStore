package com.artemhontar.fluxdigitalstore.controller;

import com.artemhontar.fluxdigitalstore.api.model.User.LoginRequest;
import com.artemhontar.fluxdigitalstore.api.model.User.PasswordResetRequest;
import com.artemhontar.fluxdigitalstore.api.model.User.RegistrationRequest;
import com.artemhontar.fluxdigitalstore.api.security.JWTUtils;
import com.artemhontar.fluxdigitalstore.exception.*;
import com.artemhontar.fluxdigitalstore.model.Authority;
import com.artemhontar.fluxdigitalstore.model.LocalUser;
import com.artemhontar.fluxdigitalstore.model.repo.UserRepo;
import com.artemhontar.fluxdigitalstore.service.User.AuthenticationService;
import com.artemhontar.fluxdigitalstore.service.ValidationErrorsParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
@Import(AuthenticationControllerTest.TestConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private ValidationErrorsParser validationErrorsParser;

    private final String AUTH_BASE_URL = "/auth/v1";

    @TestConfiguration
    static class TestConfig {
        @Bean
        public AuthenticationService authenticationService() {
            return mock(AuthenticationService.class);
        }

        @Bean
        public ValidationErrorsParser validationErrorsParser() {
            return mock(ValidationErrorsParser.class);
        }

        @Bean
        public UserRepo userRepo() {
            return mock(UserRepo.class);
        }

        @Bean
        public JWTUtils jwtUtils() {
            return mock(JWTUtils.class);
        }
    }

    // --- REGISTER Tests ---

    @Test
    void registerUser_Success_Returns201Created() throws Exception {
        RegistrationRequest request = new RegistrationRequest("test@example.com", "SecurePass123");

        doNothing().when(authenticationService).registerUser(any(RegistrationRequest.class), any(BindingResult.class));

        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(authenticationService, times(1)).registerUser(any(RegistrationRequest.class), any(BindingResult.class));
    }

    @Test
    void registerUser_UserAlreadyExists_Returns409Conflict() throws Exception {
        RegistrationRequest request = new RegistrationRequest("test@example.com", "SecurePass123");

        doThrow(new UserAlreadyExist()).when(authenticationService).registerUser(any(RegistrationRequest.class), any(BindingResult.class));

        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void registerUser_ValidationError_Returns400BadRequestAndErrors() throws Exception {
        RegistrationRequest invalidRequest = new RegistrationRequest("bad-email", "pass");

        Map<String, List<String>> validationErrors = Map.of(
                "email", List.of("must be a well-formed email address"),
                "password", List.of("size must be between 8 and 32")
        );

        when(validationErrorsParser.parseErrorsFrom(any(BindingResult.class))).thenReturn(validationErrors);

        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email[0]").isNotEmpty())
                .andExpect(jsonPath("$.password[0]").isNotEmpty());

        verify(validationErrorsParser, times(1)).parseErrorsFrom(any(BindingResult.class));
    }

    // --- LOGIN Tests ---

    @Test
    void loginUser_Success_Returns200OkAndJwt() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "password");
        String mockJwt = "mock-jwt-token";

        when(authenticationService.loginUser(any(LoginRequest.class))).thenReturn(mockJwt);

        mockMvc.perform(post(AUTH_BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(mockJwt))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void loginUser_UserNotVerified_NewEmailSent_Returns403Forbidden() throws Exception {
        LoginRequest request = new LoginRequest("unverified@example.com", "password");

        UserNotVerifiedException ex = new UserNotVerifiedException(true);
        when(authenticationService.loginUser(any(LoginRequest.class))).thenThrow(ex);

        mockMvc.perform(post(AUTH_BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.failureReason").value("USER_NOT_VERIFIED_NEW_EMAIL_SENT"));
    }

    @Test
    void loginUser_EmailFailure_Returns500InternalServerError() throws Exception {
        LoginRequest request = new LoginRequest("unverified@example.com", "password");

        when(authenticationService.loginUser(any(LoginRequest.class))).thenThrow(new EmailFailureException("Mock email sending failed during login."));

        mockMvc.perform(post(AUTH_BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // --- VERIFY Tests ---

    @Test
    void verifyUser_Success_Returns200Ok() throws Exception {
        String token = "valid-token";
        when(authenticationService.verifyUser(token)).thenReturn(true);

        mockMvc.perform(post(AUTH_BASE_URL + "/verify")
                        .param("token", token))
                .andExpect(status().isOk());
    }

    // --- FORGOT PASSWORD Tests ---

    @Test
    void forgotPassword_PasswordResetCooldown_Returns409Conflict() throws Exception {
        String email = "cooldown@example.com";
        doThrow(new PasswordResetCooldown()).when(authenticationService).trySendResetPasswordEmail(email);

        mockMvc.perform(post(AUTH_BASE_URL + "/forgot_password")
                        .param("email", email))
                .andExpect(status().isConflict())
                .andExpect(content().string("PASSWORD_RESET_COOLDOWN"));
    }

    // --- RESET PASSWORD Tests ---

    @Test
    void resetUserPassword_Success_Returns200Ok() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest("NewSecurePassword123", "token");

        doNothing().when(authenticationService).resetUserPassword(any(PasswordResetRequest.class), any(BindingResult.class));

        mockMvc.perform(post(AUTH_BASE_URL + "/reset_password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void resetUserPassword_InvalidToken_Returns400BadRequest() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest("NewSecurePassword123", "bad-token");

        doThrow(new InvalidTokenException()).when(authenticationService).resetUserPassword(any(PasswordResetRequest.class), any(BindingResult.class));

        mockMvc.perform(post(AUTH_BASE_URL + "/reset_password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("INVALID_OR_EXPIRED_TOKEN"));
    }

    @Test
    void resetUserPassword_PasswordValidationFailed_Returns400BadRequest() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest("short", "token");
        Map<String, List<String>> validationErrors = Map.of("newPassword", List.of("size must be between 8 and 64"));

        doThrow(new DetaiIsNotVerified(validationErrors)).when(authenticationService).resetUserPassword(any(PasswordResetRequest.class), any(BindingResult.class));

        mockMvc.perform(post(AUTH_BASE_URL + "/reset_password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.newPassword[0]").value("size must be between 8 and 64"));
    }

    // --- GET USER INFO Tests ---

    @Test
    void getUserInfo_UserFound_Returns200OkWithUserInfo() throws Exception {
        Authority roleUser = new Authority();
        roleUser.setAuthorityName("ROLE_USER");
        Authority roleAdmin = new Authority();
        roleAdmin.setAuthorityName("ROLE_ADMIN");

        LocalUser mockUser = new LocalUser();
        mockUser.setId(1L);
        mockUser.setEmail("user@example.com");
        mockUser.setAuthorities(Set.of(roleUser, roleAdmin));

        when(authenticationService.tryGetCurrentUser()).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get(AUTH_BASE_URL + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("ROLE_USER", "ROLE_ADMIN")));

        verify(authenticationService, times(1)).tryGetCurrentUser();
    }
}