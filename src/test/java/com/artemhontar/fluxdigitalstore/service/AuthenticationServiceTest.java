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

    /**
     * Sets up mock data, users, and necessary mocking for dependencies
     * before each test execution.
     */
    @BeforeEach
    void setUp() {
        mockVerifiedUser = spy(new LocalUser());
        mockVerifiedUser.setId(1L);
        mockVerifiedUser.setEmail(TEST_EMAIL);
        mockVerifiedUser.setPassword(ENCODED_PASSWORD);
        mockVerifiedUser.setEmailVerified(true);
        mockVerifiedUser.setVerificationTokens(Collections.emptyList());

        mockUnverifiedUser = new LocalUser();
        mockUnverifiedUser.setId(2L);
        mockUnverifiedUser.setEmail(UNVERIFIED_EMAIL);
        mockUnverifiedUser.setPassword(ENCODED_PASSWORD);
        mockUnverifiedUser.setEmailVerified(false);
        mockUnverifiedUser.setVerificationTokens(Collections.emptyList());

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
    }

    // ===================================
    // TEST: registerUser
    // ===================================

    /**
     * Tests successful user registration, verifying that the user is saved,
     * a verification token is created, and the confirmation email is sent.
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
     * Tests login attempt by an unverified user within the email resend cooldown period.
     * Should throw {@link UserNotVerifiedException} but not resend the email.
     *
     * @throws EmailFailureException if the email service mock setup failed.
     */
    @Test
    void loginUser_UnverifiedUserNoResend_ThrowsException() throws EmailFailureException {
        VerificationToken token = new VerificationToken();
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
    void trySendResetPasswordEmail_UserNotVerified_ThrowsException() throws EmailsNotVerifiedException {
        when(userRepository.findByEmailIgnoreCase(UNVERIFIED_EMAIL)).thenReturn(Optional.of(mockUnverifiedUser));

        assertThrows(EmailsNotVerifiedException.class, () -> {
            authenticationService.trySendResetPasswordEmail(UNVERIFIED_EMAIL);
        });

        verify(rptService, never()).tryToCreateRPT(any());
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

    /**
     * Tests the security context check for the 'ROLE_ADMIN' authority.
     */
    @Test
    void isAdmin_IsAdmin_ReturnsTrue() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        GrantedAuthority adminAuthority = new SimpleGrantedAuthority("ROLE_ADMIN");

        doReturn(Collections.singletonList(adminAuthority)).when(authentication).getAuthorities();

        boolean result = authenticationService.isAdmin();

        assertTrue(result);
    }
}