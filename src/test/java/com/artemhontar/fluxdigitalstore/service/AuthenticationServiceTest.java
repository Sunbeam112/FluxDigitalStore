package com.artemhontar.fluxdigitalstore.service;

import com.artemhontar.fluxdigitalstore.api.model.User.LoginRequest;
import com.artemhontar.fluxdigitalstore.api.model.User.RegistrationRequest;
import com.artemhontar.fluxdigitalstore.api.security.JWTUtils;
import com.artemhontar.fluxdigitalstore.exception.*;
import com.artemhontar.fluxdigitalstore.model.Authority;
import com.artemhontar.fluxdigitalstore.model.LocalUser;
import com.artemhontar.fluxdigitalstore.model.ResetPasswordToken;
import com.artemhontar.fluxdigitalstore.model.VerificationToken;
import com.artemhontar.fluxdigitalstore.model.repo.AuthorityRepository; // New Dependency
import com.artemhontar.fluxdigitalstore.model.repo.UserRepo;
import com.artemhontar.fluxdigitalstore.model.repo.VerificationTokenRepository;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link AuthenticationService} class.
 * These tests use Mockito to simulate the behavior of dependencies like
 * repositories, utility classes, and email services, ensuring that the
 * AuthenticationService's business logic is correct under various conditions.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepo userRepository;
    @Mock
    private JWTUtils jwtUtils;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private VerificationTokenRepository verificationTokenRepository;
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

    // NEW MOCKS ADDED TO MATCH UPDATED CONSTRUCTOR
    @Mock
    private ValidationErrorsParser validationErrorsParser;
    @Mock
    private AuthorityRepository authorityRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    // --- Mock Data ---
    private LocalUser mockVerifiedUser;
    private LocalUser mockUnverifiedUser;
    private LoginRequest mockLoginRequest;
    private RegistrationRequest mockRegistrationRequest;
    private ResetPasswordToken mockRPT;
    private Authority mockUserAuthority;
    private final String TEST_EMAIL = "test@mail.com";
    private final String UNVERIFIED_EMAIL = "unverified@mail.com";
    private final String TEST_PASSWORD = "password";
    private final String ENCODED_PASSWORD = "encoded_password_hash";
    private final String MOCK_JWT = "mock_jwt_token";

    /**
     * Sets up mock data, users, and necessary mocking for dependencies
     * before each test execution.
     */
    @BeforeEach
    void setUp() {
        mockUserAuthority = new Authority();
        mockUserAuthority.setAuthorityName("ROLE_USER");

        mockVerifiedUser = spy(new LocalUser());
        mockVerifiedUser.setId(1L);
        mockVerifiedUser.setEmail(TEST_EMAIL);
        mockVerifiedUser.setPassword(ENCODED_PASSWORD);
        mockVerifiedUser.setEmailVerified(true);
        mockVerifiedUser.setVerificationTokens(Collections.emptyList());
        mockVerifiedUser.setAuthorities(Set.of(mockUserAuthority));

        mockUnverifiedUser = new LocalUser();
        mockUnverifiedUser.setId(2L);
        mockUnverifiedUser.setEmail(UNVERIFIED_EMAIL);
        mockUnverifiedUser.setPassword(ENCODED_PASSWORD);
        mockUnverifiedUser.setEmailVerified(false);
        mockUnverifiedUser.setVerificationTokens(Collections.emptyList());
        mockUnverifiedUser.setAuthorities(Set.of(mockUserAuthority));

        mockLoginRequest = new LoginRequest();
        mockLoginRequest.setEmail(TEST_EMAIL);
        mockLoginRequest.setPassword(TEST_PASSWORD);

        mockRegistrationRequest = new RegistrationRequest();
        mockRegistrationRequest.setEmail(TEST_EMAIL);
        mockRegistrationRequest.setPassword(TEST_PASSWORD);

        mockRPT = new ResetPasswordToken();
        mockRPT.setToken("reset_token");

        SecurityContextHolder.setContext(securityContext);

        // Stub email services to prevent checked EmailFailureException when not the focus of the test.
        try {
            lenient().doNothing().when(emailVerificationService).sendEmailConformationMessage(any(VerificationToken.class));
            lenient().doNothing().when(resetPasswordEmailService).sendResetPasswordEmail(any(ResetPasswordToken.class));
        } catch (EmailFailureException e) {
            throw new RuntimeException("Mock setup failed for email services.", e);
        }

        // Stub the default role lookup for registration success test
        lenient().when(authorityRepository.findByAuthorityName("ROLE_USER")).thenReturn(Optional.of(mockUserAuthority));
    }

    // ===================================
    // TEST: registerUser
    // ===================================

    /**
     * Tests successful user registration, verifying that the user is saved,
     * a verification token is created, and the confirmation email is sent.
     * The new AuthorityRepository dependency is verified here.
     *
     * @throws Exception if an unexpected exception occurs during the registration process.
     */
    @Test
    void registerUser_Success_SavesUserAndSendsEmail() throws Exception {
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(verificationTokenService.createVerificationToken(any(LocalUser.class)))
                .thenReturn(new VerificationToken());

        authenticationService.registerUser(mockRegistrationRequest, bindingResult);

        // Verify Authority is looked up
        verify(authorityRepository, times(1)).findByAuthorityName("ROLE_USER");
        // Verify core registration steps
        verify(userRepository, times(1)).findByEmailIgnoreCase(TEST_EMAIL);
        verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
        verify(userRepository, times(1)).save(any(LocalUser.class));
        verify(verificationTokenRepository, times(1)).save(any(VerificationToken.class));
        verify(emailVerificationService, times(1)).sendEmailConformationMessage(any(VerificationToken.class));
    }

    /**
     * Tests registration failure when a user with the given email already exists,
     * ensuring a {@link UserAlreadyExist} exception is thrown.
     */
    @Test
    void registerUser_UserAlreadyExists_ThrowsException() {
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockVerifiedUser));

        assertThrows(UserAlreadyExist.class, () -> {
            authenticationService.registerUser(mockRegistrationRequest, bindingResult);
        });

        verify(authorityRepository, never()).findByAuthorityName(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(LocalUser.class));
    }

    /**
     * Tests registration failure when the default role is missing from the database,
     * ensuring a RuntimeException is thrown.
     */
    @Test
    void registerUser_DefaultRoleMissing_ThrowsRuntimeException() {
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.empty());
        when(authorityRepository.findByAuthorityName("ROLE_USER")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authenticationService.registerUser(mockRegistrationRequest, bindingResult);
        });

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(LocalUser.class));
    }

    // ===================================
    // TEST: loginUser
    // ===================================

    /**
     * Tests successful login for a verified user, ensuring a JWT is returned.
     *
     * @throws Exception if an unexpected exception occurs during login.
     */
    @Test
    void loginUser_VerifiedUserSuccess_ReturnsJWT() throws Exception {
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockVerifiedUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtUtils.generateToken(TEST_EMAIL)).thenReturn(MOCK_JWT);

        String token = authenticationService.loginUser(mockLoginRequest);

        assertEquals(MOCK_JWT, token);
        // Note: AuthenticationService uses user.getUsername() which is email
        verify(jwtUtils, times(1)).generateToken(TEST_EMAIL);
    }

    /**
     * Tests login failure when the provided password does not match the stored hash.
     *
     * @throws Exception if an unexpected exception occurs during login.
     */
    @Test
    void loginUser_IncorrectPassword_ReturnsNull() throws Exception {
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockVerifiedUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

        String token = authenticationService.loginUser(mockLoginRequest);

        assertNull(token);
        verify(jwtUtils, never()).generateToken(anyString());
    }

    /**
     * Tests login failure when the user does not exist.
     *
     * @throws Exception if an unexpected exception occurs during login.
     */
    @Test
    void loginUser_UserNotFound_ReturnsNull() throws Exception {
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.empty());

        String token = authenticationService.loginUser(mockLoginRequest);

        assertNull(token);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtils, never()).generateToken(anyString());
    }


    /**
     * Tests login attempt by an unverified user within the email resend cooldown period.
     * Should throw {@link UserNotVerifiedException} but not resend the email.
     *
     * @throws EmailFailureException if the email service mock setup failed.
     */
    @Test
    void loginUser_UnverifiedUserNoResend_ThrowsException() throws EmailFailureException {
        VerificationToken token = new VerificationToken();
        // Cooldown is 5 minutes (300000 ms), this is 10 seconds ago
        token.setCreatedTimestamp(new Timestamp(System.currentTimeMillis() - 10000));
        mockUnverifiedUser.setVerificationTokens(List.of(token));

        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(mockUnverifiedUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        UserNotVerifiedException exception = assertThrows(UserNotVerifiedException.class, () -> {
            authenticationService.loginUser(mockLoginRequest);
        });

        assertFalse(exception.isNewEmailSent());
        verify(emailVerificationService, never()).sendEmailConformationMessage(any());
        verify(verificationTokenRepository, never()).save(any());
    }

    /**
     * Tests login attempt by an unverified user after the email resend cooldown has passed.
     * Should send a new verification email and throw {@link UserNotVerifiedException} with `isNewEmailSent` true.
     *
     * @throws EmailFailureException if the email service mock setup failed.
     */
    @Test
    void loginUser_UnverifiedUserWithResend_SendsEmailAndThrowsException() throws EmailFailureException {
        VerificationToken oldToken = new VerificationToken();
        // Cooldown is 5 minutes (300000 ms), this is 6 minutes ago
        oldToken.setCreatedTimestamp(new Timestamp(System.currentTimeMillis() - 360000));
        mockUnverifiedUser.setVerificationTokens(List.of(oldToken));

        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(mockUnverifiedUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(verificationTokenService.createVerificationToken(any())).thenReturn(new VerificationToken());

        UserNotVerifiedException exception = assertThrows(UserNotVerifiedException.class, () -> {
            authenticationService.loginUser(mockLoginRequest);
        });

        assertTrue(exception.isNewEmailSent());
        verify(emailVerificationService, times(1)).sendEmailConformationMessage(any());
        verify(verificationTokenRepository, times(1)).save(any());
    }

    // ===================================
    // TEST: verifyUser
    // ===================================

    /**
     * Tests successful user verification using a valid token, ensuring the user's
     * verified flag is set and the old tokens are deleted.
     */
    @Test
    void verifyUser_Success_VerifiesUserAndDeletesToken() {
        VerificationToken token = new VerificationToken();
        token.setLocalUser(mockUnverifiedUser);
        when(verificationTokenRepository.findByToken("valid_token")).thenReturn(Optional.of(token));

        boolean result = authenticationService.verifyUser("valid_token");

        assertTrue(result);
        assertTrue(mockUnverifiedUser.isEmailVerified());
        verify(userRepository, times(1)).save(mockUnverifiedUser);
        verify(verificationTokenRepository, times(1)).deleteByLocalUser(mockUnverifiedUser);
    }

    /**
     * Tests verification when the user is already verified. Should return true but not re-save or re-delete.
     */
    @Test
    void verifyUser_AlreadyVerified_ReturnsTrueAndDoesNothing() {
        VerificationToken token = new VerificationToken();
        token.setLocalUser(mockVerifiedUser);
        when(verificationTokenRepository.findByToken("valid_token")).thenReturn(Optional.of(token));

        boolean result = authenticationService.verifyUser("valid_token");

        assertTrue(result);
        verify(userRepository, never()).save(any(LocalUser.class));
        verify(verificationTokenRepository, never()).deleteByLocalUser(any(LocalUser.class));
    }

    // ===================================
    // TEST: setUserPasswordByEmail
    // ===================================

    /**
     * Tests successful setting of a new password for a verified user,
     * ensuring the password hash is updated and the user is saved.
     *
     * @throws EmailsNotVerifiedException should not be thrown in this success case.
     */
    @Test
    void setUserPasswordByEmail_Success_UpdatesPasswordAndReturnsTrue() throws EmailsNotVerifiedException {
        String newPassword = "new_password";
        String newHash = "new_password_hash";
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockVerifiedUser));
        when(passwordEncoder.encode(newPassword)).thenReturn(newHash);

        boolean result = authenticationService.setUserPasswordByEmail(TEST_EMAIL, newPassword, bindingResult);

        assertTrue(result);
        verify(mockVerifiedUser, atLeastOnce()).setPassword(newHash);
        verify(userRepository, times(1)).save(mockVerifiedUser);
    }

    /**
     * Tests password setting failure for an unverified user, ensuring an
     * {@link EmailsNotVerifiedException} is thrown.
     */
    @Test
    void setUserPasswordByEmail_NotVerified_ThrowsException() {
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase(UNVERIFIED_EMAIL)).thenReturn(Optional.of(mockUnverifiedUser));

        assertThrows(EmailsNotVerifiedException.class, () -> {
            authenticationService.setUserPasswordByEmail(UNVERIFIED_EMAIL, TEST_PASSWORD, bindingResult);
        });

        verify(userRepository, never()).save(any());
    }

    /**
     * Tests password setting failure due to validation errors on the binding result.
     *
     * @throws EmailsNotVerifiedException should not be thrown as validation fails first.
     */
    @Test
    void setUserPasswordByEmail_ValidationErrors_ReturnsFalse() throws EmailsNotVerifiedException {
        when(bindingResult.hasErrors()).thenReturn(true);

        boolean result = authenticationService.setUserPasswordByEmail(TEST_EMAIL, TEST_PASSWORD, bindingResult);

        assertFalse(result);
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    /**
     * Tests password setting failure when user is not found.
     *
     * @throws EmailsNotVerifiedException should not be thrown.
     */
    @Test
    void setUserPasswordByEmail_UserNotFound_ReturnsFalse() throws EmailsNotVerifiedException {
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("notfound@mail.com")).thenReturn(Optional.empty());

        boolean result = authenticationService.setUserPasswordByEmail("notfound@mail.com", TEST_PASSWORD, bindingResult);

        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    // ===================================
    // TEST: trySendResetPasswordEmail
    // ===================================

    /**
     * Tests successful sending of a reset password email for a verified user,
     * verifying that a new RPT is created and the email service is called.
     *
     * @throws Exception if an unexpected exception occurs.
     */
    @Test
    void trySendResetPasswordEmail_Success_SendsEmail() throws Exception {
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockVerifiedUser));
        when(rptService.tryToCreateRPT(mockVerifiedUser)).thenReturn(mockRPT);

        authenticationService.trySendResetPasswordEmail(TEST_EMAIL);

        verify(rptService, times(1)).tryToCreateRPT(mockVerifiedUser);
        verify(resetPasswordEmailService, times(1)).sendResetPasswordEmail(mockRPT);
    }

    /**
     * Tests failure to send a reset password email for an unverified user,
     * ensuring an {@link EmailsNotVerifiedException} is thrown.
     *
     * @throws EmailsNotVerifiedException should be thrown in this case.
     */
    @Test
    void trySendResetPasswordEmail_UserNotVerified_ThrowsException() throws EmailsNotVerifiedException, EmailFailureException {
        when(userRepository.findByEmailIgnoreCase(UNVERIFIED_EMAIL)).thenReturn(Optional.of(mockUnverifiedUser));

        assertThrows(EmailsNotVerifiedException.class, () -> {
            authenticationService.trySendResetPasswordEmail(UNVERIFIED_EMAIL);
        });

        verify(rptService, never()).tryToCreateRPT(any());
        verify(resetPasswordEmailService, never()).sendResetPasswordEmail(any());
    }

    /**
     * Tests silent failure when requesting a reset for a non-existent user.
     *
     * @throws Exception should not be thrown.
     */
    @Test
    void trySendResetPasswordEmail_UserNotFound_SilentlyFails() throws Exception {
        when(userRepository.findByEmailIgnoreCase("notfound@mail.com")).thenReturn(Optional.empty());

        // Should not throw any exception
        assertDoesNotThrow(() -> authenticationService.trySendResetPasswordEmail("notfound@mail.com"));

        verify(rptService, never()).tryToCreateRPT(any());
        verify(resetPasswordEmailService, never()).sendResetPasswordEmail(any());
    }

    /**
     * Tests failure due to a password reset cooldown being active,
     * ensuring a {@link PasswordResetCooldown} exception is thrown.
     *
     * @throws Exception if an unexpected exception occurs.
     */
    @Test
    void trySendResetPasswordEmail_PasswordResetCooldown_ThrowsException() throws Exception {
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockVerifiedUser));
        when(rptService.tryToCreateRPT(mockVerifiedUser)).thenThrow(new PasswordResetCooldown());

        assertThrows(PasswordResetCooldown.class, () -> {
            authenticationService.trySendResetPasswordEmail(TEST_EMAIL);
        });

        verify(resetPasswordEmailService, never()).sendResetPasswordEmail(any());
    }

    // ===================================
    // TEST: tryGetCurrentUser
    // ===================================

    /**
     * Tests retrieval of the current user from the SecurityContext.
     */
    @Test
    void tryGetCurrentUser_UserAuthenticated_ReturnsUser() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockVerifiedUser));

        Optional<LocalUser> result = authenticationService.tryGetCurrentUser();

        assertTrue(result.isPresent());
        assertEquals(TEST_EMAIL, result.get().getEmail());
    }

    /**
     * Tests failure to retrieve current user when principal is null.
     */
    @Test
    void tryGetCurrentUser_PrincipalIsNull_ReturnsEmpty() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(null);

        Optional<LocalUser> result = authenticationService.tryGetCurrentUser();

        assertFalse(result.isPresent());
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }


    // ===================================
    // TEST: Utility/Security Context
    // ===================================

    /**
     * Tests the utility method for checking if a user exists by email.
     */
    @Test
    void isUserExistsByEmail_Exists_ReturnsTrue() {
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockVerifiedUser));
        assertTrue(authenticationService.isUserExistsByEmail(TEST_EMAIL));
    }

    /**
     * Tests the utility method for checking if a user exists by ID.
     */
    @Test
    void isUserExistsByID_NotExists_ReturnsFalse() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertFalse(authenticationService.isUserExistsByID(99L));
    }

    /**
     * Tests the utility method for checking if a user's email is verified.
     */
    @Test
    void isUserEmailVerified_Verified_ReturnsTrue() {
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockVerifiedUser));
        assertTrue(authenticationService.isUserEmailVerified(TEST_EMAIL));
    }

}