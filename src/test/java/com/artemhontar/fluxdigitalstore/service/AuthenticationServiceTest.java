package com.artemhontar.fluxdigitalstore.service;

import com.artemhontar.fluxdigitalstore.api.model.User.LoginRequest;
import com.artemhontar.fluxdigitalstore.api.model.User.RegistrationRequest;
import com.artemhontar.fluxdigitalstore.api.security.JWTUtils;
import com.artemhontar.fluxdigitalstore.exception.*;
import com.artemhontar.fluxdigitalstore.model.LocalUser;
import com.artemhontar.fluxdigitalstore.model.ResetPasswordToken;
import com.artemhontar.fluxdigitalstore.model.VerificationToken;
import com.artemhontar.fluxdigitalstore.repo.UserRepo;
import com.artemhontar.fluxdigitalstore.repo.verificationTokenRepository;
import com.artemhontar.fluxdigitalstore.service.Email.EmailVerificationService;
import com.artemhontar.fluxdigitalstore.service.Email.ResetPasswordEmailService;
import com.artemhontar.fluxdigitalstore.service.User.AuthenticationService;
import com.artemhontar.fluxdigitalstore.service.User.RPTService;
import com.artemhontar.fluxdigitalstore.service.User.VerificationTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
// The static import for 'lenient()' is already included via 'import static org.mockito.Mockito.*;'
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepo userRepository;
    @Mock
    private JWTUtils jwtUtils;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private verificationTokenRepository verificationTokenRepository;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private RPTService rptService;
    @Mock
    private ResetPasswordEmailService resetPasswordEmailService;
    @Mock
    private VerificationTokenService verificationTokenService;
    @Mock
    private BindingResult bindingResult;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthenticationService authenticationService;

    // --- Mock Data ---
    private LocalUser mockVerifiedUser;
    private LocalUser mockUnverifiedUser;
    private LoginRequest mockLoginRequest;
    private RegistrationRequest mockRegistrationRequest;
    private ResetPasswordToken mockRPT;
    private final String TEST_EMAIL = "test@mail.com";
    private final String UNVERIFIED_EMAIL = "unverified@mail.com";
    private final String TEST_PASSWORD = "password";
    private final String ENCODED_PASSWORD = "encoded_password_hash";
    private final String MOCK_JWT = "mock_jwt_token";

    @BeforeEach
    void setUp() {
        // Mock Verified User (use spy to allow verification of setter calls later)
        mockVerifiedUser = spy(new LocalUser());
        mockVerifiedUser.setId(1L);
        mockVerifiedUser.setEmail(TEST_EMAIL);
        mockVerifiedUser.setPassword(ENCODED_PASSWORD);
        mockVerifiedUser.setEmailVerified(true);
        mockVerifiedUser.setVerificationTokens(Collections.emptyList());

        // Mock Unverified User
        mockUnverifiedUser = new LocalUser();
        mockUnverifiedUser.setId(2L);
        mockUnverifiedUser.setEmail(UNVERIFIED_EMAIL);
        mockUnverifiedUser.setPassword(ENCODED_PASSWORD);
        mockUnverifiedUser.setEmailVerified(false);
        mockUnverifiedUser.setVerificationTokens(Collections.emptyList());

        // Mock Requests
        mockLoginRequest = new LoginRequest();
        mockLoginRequest.setEmail(TEST_EMAIL);
        mockLoginRequest.setPassword(TEST_PASSWORD);

        mockRegistrationRequest = new RegistrationRequest();
        mockRegistrationRequest.setEmail(TEST_EMAIL);
        mockRegistrationRequest.setPassword(TEST_PASSWORD);

        // Mock RPT
        mockRPT = new ResetPasswordToken();
        mockRPT.setToken("reset_token");

        // Setup SecurityContext mocking for tryGetCurrentUser/isAdmin
        SecurityContextHolder.setContext(securityContext);

        // FIX: Use lenient() to prevent UnnecessaryStubbingException.
        // These stubs are required to prevent checked EmailFailureException in tests
        // where these methods are *not* expected to be called.
        try {
            lenient().doNothing().when(emailVerificationService).sendEmailConformationMessage(any(VerificationToken.class));
            lenient().doNothing().when(resetPasswordEmailService).sendResetPasswordEmail(any(ResetPasswordToken.class));
        } catch (EmailFailureException e) {
            throw new RuntimeException("Mock setup failed for email services.", e);
        }
    }

    // ===================================
    // TEST: registerUser
    // ===================================

    @Test
    void registerUser_Success_SavesUserAndSendsEmail() throws Exception {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(verificationTokenService.createVerificationToken(any(LocalUser.class)))
                .thenReturn(new VerificationToken());

        // Act
        authenticationService.registerUser(mockRegistrationRequest, bindingResult);

        // Assert
        verify(userRepository, times(1)).findByEmailIgnoreCase(TEST_EMAIL);
        verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
        verify(userRepository, times(1)).save(any(LocalUser.class));
        verify(verificationTokenRepository, times(1)).save(any(VerificationToken.class));
        verify(emailVerificationService, times(1)).sendEmailConformationMessage(any(VerificationToken.class));
    }

    @Test
    void registerUser_UserAlreadyExists_ThrowsException() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockVerifiedUser));

        // Act & Assert
        assertThrows(UserAlreadyExist.class, () -> {
            authenticationService.registerUser(mockRegistrationRequest, bindingResult);
        });

        // Assert that no other methods were called
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(LocalUser.class));
    }

    // ===================================
    // TEST: loginUser
    // ===================================

    @Test
    void loginUser_VerifiedUserSuccess_ReturnsJWT() throws Exception {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockVerifiedUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtUtils.generateToken(TEST_EMAIL)).thenReturn(MOCK_JWT);

        // Act
        String token = authenticationService.loginUser(mockLoginRequest);

        // Assert
        assertEquals(MOCK_JWT, token);
        verify(jwtUtils, times(1)).generateToken(TEST_EMAIL);
    }

    @Test
    void loginUser_IncorrectPassword_ReturnsNull() throws Exception {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockVerifiedUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

        // Act
        String token = authenticationService.loginUser(mockLoginRequest);

        // Assert
        assertNull(token);
        verify(jwtUtils, never()).generateToken(anyString());
    }

    @Test
    void loginUser_UnverifiedUserNoResend_ThrowsException() throws EmailFailureException {
        // Arrange
        VerificationToken token = new VerificationToken();
        // Token created 10 seconds ago (Within 5-minute cooldown)
        token.setCreatedTimestamp(new Timestamp(System.currentTimeMillis() - 10000));
        mockUnverifiedUser.setVerificationTokens(List.of(token));

        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(mockUnverifiedUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // Act & Assert
        UserNotVerifiedException exception = assertThrows(UserNotVerifiedException.class, () -> {
            authenticationService.loginUser(mockLoginRequest);
        });

        // Assert
        assertFalse(exception.isNewEmailSent(), "Should not send a new email because of cooldown.");
        verify(emailVerificationService, never()).sendEmailConformationMessage(any());
        verify(verificationTokenRepository, never()).save(any());
    }

    @Test
    void loginUser_UnverifiedUserWithResend_SendsEmailAndThrowsException() throws EmailFailureException {
        // Arrange
        VerificationToken oldToken = new VerificationToken();
        // Token created 6 minutes ago (Outside 5-minute cooldown)
        oldToken.setCreatedTimestamp(new Timestamp(System.currentTimeMillis() - 360000));
        mockUnverifiedUser.setVerificationTokens(List.of(oldToken));

        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(mockUnverifiedUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(verificationTokenService.createVerificationToken(any())).thenReturn(new VerificationToken());

        // Act & Assert
        UserNotVerifiedException exception = assertThrows(UserNotVerifiedException.class, () -> {
            authenticationService.loginUser(mockLoginRequest);
        });

        // Assert
        assertTrue(exception.isNewEmailSent(), "Should send a new email because cooldown has passed.");
        verify(emailVerificationService, times(1)).sendEmailConformationMessage(any());
        verify(verificationTokenRepository, times(1)).save(any());
    }

    // ===================================
    // TEST: verifyUser
    // ===================================

    @Test
    void verifyUser_Success_VerifiesUserAndDeletesToken() {
        // Arrange
        VerificationToken token = new VerificationToken();
        token.setLocalUser(mockUnverifiedUser);
        when(verificationTokenRepository.findByToken("valid_token")).thenReturn(Optional.of(token));

        // Act
        boolean result = authenticationService.verifyUser("valid_token");

        // Assert
        assertTrue(result);
        assertTrue(mockUnverifiedUser.isEmailVerified());
        verify(userRepository, times(1)).save(mockUnverifiedUser);
        verify(verificationTokenRepository, times(1)).deleteByLocalUser(mockUnverifiedUser);
    }

    // ===================================
    // TEST: setUserPasswordByEmail
    // ===================================

    @Test
    void setUserPasswordByEmail_Success_UpdatesPasswordAndReturnsTrue() throws EmailsNotVerifiedException {
        // Arrange
        String newPassword = "new_password";
        String newHash = "new_password_hash";
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockVerifiedUser));
        when(passwordEncoder.encode(newPassword)).thenReturn(newHash);

        // Act
        boolean result = authenticationService.setUserPasswordByEmail(TEST_EMAIL, newPassword, bindingResult);

        // Assert
        assertTrue(result);
        // Verify setter called on the spy
        verify(mockVerifiedUser, atLeastOnce()).setPassword(newHash);
        verify(userRepository, times(1)).save(mockVerifiedUser);
    }

    @Test
    void setUserPasswordByEmail_NotVerified_ThrowsException() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase(UNVERIFIED_EMAIL)).thenReturn(Optional.of(mockUnverifiedUser));

        // Act & Assert
        assertThrows(EmailsNotVerifiedException.class, () -> {
            authenticationService.setUserPasswordByEmail(UNVERIFIED_EMAIL, TEST_PASSWORD, bindingResult);
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    void setUserPasswordByEmail_ValidationErrors_ReturnsFalse() throws EmailsNotVerifiedException {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(true);

        // Act
        boolean result = authenticationService.setUserPasswordByEmail(TEST_EMAIL, TEST_PASSWORD, bindingResult);

        // Assert
        assertFalse(result);
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    // ===================================
    // TEST: trySendResetPasswordEmail
    // ===================================

    @Test
    void trySendResetPasswordEmail_Success_SendsEmail() throws Exception {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockVerifiedUser));
        when(rptService.tryToCreateRPT(mockVerifiedUser)).thenReturn(mockRPT);

        // Act
        authenticationService.trySendResetPasswordEmail(TEST_EMAIL);

        // Assert
        verify(rptService, times(1)).tryToCreateRPT(mockVerifiedUser);
        verify(resetPasswordEmailService, times(1)).sendResetPasswordEmail(mockRPT);
    }

    @Test
    void trySendResetPasswordEmail_UserNotVerified_ThrowsException() throws EmailsNotVerifiedException {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(UNVERIFIED_EMAIL)).thenReturn(Optional.of(mockUnverifiedUser));

        // Act & Assert
        assertThrows(EmailsNotVerifiedException.class, () -> {
            authenticationService.trySendResetPasswordEmail(UNVERIFIED_EMAIL);
        });

        verify(rptService, never()).tryToCreateRPT(any());
    }

    @Test
    void trySendResetPasswordEmail_PasswordResetCooldown_ThrowsException() throws Exception {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockVerifiedUser));
        // Mock RPTService to hit cooldown
        when(rptService.tryToCreateRPT(mockVerifiedUser)).thenThrow(new PasswordResetCooldown());

        // Act & Assert
        assertThrows(PasswordResetCooldown.class, () -> {
            authenticationService.trySendResetPasswordEmail(TEST_EMAIL);
        });

        verify(resetPasswordEmailService, never()).sendResetPasswordEmail(any());
    }

    // ===================================
    // TEST: Utility/Security Context
    // ===================================

    @Test
    void isUserExistsByEmail_Exists_ReturnsTrue() {
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockVerifiedUser));
        assertTrue(authenticationService.isUserExistsByEmail(TEST_EMAIL));
    }

    @Test
    void isUserExistsByID_NotExists_ReturnsFalse() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertFalse(authenticationService.isUserExistsByID(99L));
    }

    @Test
    void isUserEmailVerified_Verified_ReturnsTrue() {
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockVerifiedUser));
        assertTrue(authenticationService.isUserEmailVerified(TEST_EMAIL));
    }

    @Test
    void isAdmin_IsAdmin_ReturnsTrue() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        GrantedAuthority adminAuthority = new SimpleGrantedAuthority("ROLE_ADMIN");

        // FIX: Using doReturn().when() to handle the generic Collection return type robustly,
        // which avoids the previous "Cannot resolve method 'thenReturn(List<T>)'" error.
        doReturn(Collections.singletonList(adminAuthority)).when(authentication).getAuthorities();

        // Act
        boolean result = authenticationService.isAdmin();

        // Assert
        assertTrue(result);
    }
}