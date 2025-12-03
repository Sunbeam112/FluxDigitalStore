package com.artemhontar.fluxdigitalstore.service;

import com.artemhontar.fluxdigitalstore.exception.EmailsNotVerifiedException;
import com.artemhontar.fluxdigitalstore.exception.PasswordResetCooldown;
import com.artemhontar.fluxdigitalstore.model.LocalUser;
import com.artemhontar.fluxdigitalstore.model.ResetPasswordToken;
import com.artemhontar.fluxdigitalstore.model.repo.RPTRepository;
import com.artemhontar.fluxdigitalstore.service.User.RPTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@code RPTService} (Reset Password Token Service) class.
 * These tests cover token creation, validation, verification, and management,
 * ensuring business rules regarding user verification, token usage, expiry,
 * and cooldown periods are enforced correctly.
 */
@ExtendWith(MockitoExtension.class)
class RPTServiceTest {

    @Mock
    private RPTRepository rtpRepository;

    @InjectMocks
    private RPTService rptService;

    // --- Constants and Mocks ---
    private static final long TOKEN_EXPIRY_MSEC = 300000L; // 5 minutes
    private static final String VALID_TOKEN_STRING = UUID.randomUUID().toString();
    private static final String INVALID_TOKEN_STRING = "short";
    private static final Long USER_ID = 1L;

    private LocalUser verifiedUser;
    private LocalUser unverifiedUser;

    /**
     * Sets up mock user data and uses reflection to manually inject the
     * {@code tokenExpiryInMillisecond} value into the service before each test.
     *
     * @throws Exception if reflection access or setting fails.
     */
    @BeforeEach
    void setUp() throws Exception {
        Field field = RPTService.class.getDeclaredField("tokenExpiryInMillisecond");
        field.setAccessible(true);
        field.set(rptService, (int) TOKEN_EXPIRY_MSEC);

        verifiedUser = new LocalUser();
        verifiedUser.setId(USER_ID);
        verifiedUser.setEmailVerified(true);
        verifiedUser.setResetPasswordTokens(new ArrayList<>());

        unverifiedUser = new LocalUser();
        unverifiedUser.setId(2L);
        unverifiedUser.setEmailVerified(false);
        unverifiedUser.setResetPasswordTokens(new ArrayList<>());
    }

    /**
     * Helper method to create a mock {@code ResetPasswordToken} with custom properties.
     *
     * @param user           The {@code LocalUser} associated with the token.
     * @param timeOffsetMsec The offset from the current time to set the expiry date (positive for future, negative for past).
     * @param isUsed         Flag indicating if the token has been used.
     * @return A mock {@code ResetPasswordToken} instance.
     */
    private ResetPasswordToken createMockToken(LocalUser user, long timeOffsetMsec, boolean isUsed) {
        ResetPasswordToken token = new ResetPasswordToken();
        token.setId(10L);
        token.setToken(VALID_TOKEN_STRING);
        token.setLocalUser(user);
        token.setIsTokenUsed(isUsed);
        token.setExpiryDateInMilliseconds(new Timestamp(System.currentTimeMillis() + timeOffsetMsec));
        return token;
    }

    // ===========================================
    // TEST: verifyAndGetRPT(String tokenInput)
    // ===========================================

    /**
     * Tests that {@code verifyAndGetRPT} returns an empty Optional for invalid token formats (null or short string).
     */
    @Test
    void verifyAndGetRPT_InvalidFormat_ReturnsEmpty() {
        assertTrue(rptService.verifyAndGetRPT(null).isEmpty());
        assertTrue(rptService.verifyAndGetRPT(INVALID_TOKEN_STRING).isEmpty());
        verify(rtpRepository, never()).getByTokenIgnoreCase(anyString());
    }

    /**
     * Tests that {@code verifyAndGetRPT} returns an empty Optional if the token is not found in the repository.
     */
    @Test
    void verifyAndGetRPT_TokenNotFound_ReturnsEmpty() {
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(null);

        assertTrue(rptService.verifyAndGetRPT(VALID_TOKEN_STRING).isEmpty());
    }

    /**
     * Tests that {@code verifyAndGetRPT} returns an empty Optional if the found token has already been used.
     */
    @Test
    void verifyAndGetRPT_TokenUsed_ReturnsEmpty() {
        ResetPasswordToken usedToken = createMockToken(verifiedUser, TOKEN_EXPIRY_MSEC, true);
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(usedToken);

        assertTrue(rptService.verifyAndGetRPT(VALID_TOKEN_STRING).isEmpty());
    }

    /**
     * Tests that {@code verifyAndGetRPT} returns an empty Optional if the found token has expired.
     */
    @Test
    void verifyAndGetRPT_TokenExpired_ReturnsEmpty() {
        ResetPasswordToken expiredToken = createMockToken(verifiedUser, -1000L, false);
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(expiredToken);

        assertTrue(rptService.verifyAndGetRPT(VALID_TOKEN_STRING).isEmpty());
    }

    /**
     * Tests that {@code verifyAndGetRPT} returns an empty Optional if the associated user's email is not verified.
     */
    @Test
    void verifyAndGetRPT_UserEmailNotVerified_ReturnsEmpty() {
        ResetPasswordToken token = createMockToken(unverifiedUser, TOKEN_EXPIRY_MSEC, false);
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(token);

        assertTrue(rptService.verifyAndGetRPT(VALID_TOKEN_STRING).isEmpty());
    }

    /**
     * Tests successful retrieval of a valid, unused, and unexpired token for a verified user.
     */
    @Test
    void verifyAndGetRPT_TokenValid_ReturnsToken() {
        ResetPasswordToken validToken = createMockToken(verifiedUser, TOKEN_EXPIRY_MSEC, false);
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(validToken);

        Optional<ResetPasswordToken> result = rptService.verifyAndGetRPT(VALID_TOKEN_STRING);

        assertTrue(result.isPresent());
        assertEquals(validToken, result.get());
    }

    // ===========================================
    // TEST: markTokenAsUsed(ResetPasswordToken token)
    // ===========================================

    /**
     * Tests that {@code markTokenAsUsed} correctly sets the usage flag and saves the token.
     */
    @Test
    void markTokenAsUsed_ValidToken_MarksAndSaves() {
        ResetPasswordToken token = createMockToken(verifiedUser, TOKEN_EXPIRY_MSEC, false);

        rptService.markTokenAsUsed(token);

        assertTrue(token.getIsTokenUsed());
        verify(rtpRepository, times(1)).save(token);
    }

    /**
     * Tests that {@code markTokenAsUsed} does nothing when given a null token.
     */
    @Test
    void markTokenAsUsed_NullToken_DoesNothing() {
        rptService.markTokenAsUsed(null);

        verify(rtpRepository, never()).save(any());
    }

    // ===========================================
    // TEST: tryToCreateRPT(LocalUser user)
    // ===========================================

    /**
     * Tests that creating a token fails with {@code IllegalArgumentException} when the user is null.
     */
    @Test
    void tryToCreateRPT_NullUser_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> rptService.tryToCreateRPT(null));
        verify(rtpRepository, never()).save(any());
    }

    /**
     * Tests that creating a token fails with {@code EmailsNotVerifiedException} when the user is unverified.
     */
    @Test
    void tryToCreateRPT_EmailNotVerified_ThrowsEmailsNotVerifiedException() {
        assertThrows(EmailsNotVerifiedException.class, () -> rptService.tryToCreateRPT(unverifiedUser));
        verify(rtpRepository, never()).save(any());
    }

    /**
     * Tests successful token creation when the user has no previous tokens.
     */
    @Test
    void tryToCreateRPT_NoPreviousTokens_CreatesNewToken() {
        when(rtpRepository.save(any(ResetPasswordToken.class))).thenAnswer(invocation -> {
            ResetPasswordToken savedToken = invocation.getArgument(0);
            savedToken.setId(11L);
            savedToken.setToken(UUID.randomUUID().toString());
            return savedToken;
        });

        ResetPasswordToken newToken = assertDoesNotThrow(() -> rptService.tryToCreateRPT(verifiedUser));

        assertNotNull(newToken);
        assertFalse(verifiedUser.getResetPasswordTokens().isEmpty());
        assertTrue(newToken.getExpiryDateInMilliseconds().after(new Timestamp(System.currentTimeMillis())));
        verify(rtpRepository, times(1)).save(any(ResetPasswordToken.class));
    }

    /**
     * Tests that token creation fails with {@code PasswordResetCooldown} when a recent token exists.
     */
    @Test
    void tryToCreateRPT_CooldownActive_ThrowsPasswordResetCooldown() {
        long lastTokenCreationTime = System.currentTimeMillis() - 60000L;
        long lastTokenExpiryTime = lastTokenCreationTime + TOKEN_EXPIRY_MSEC;

        ResetPasswordToken recentToken = new ResetPasswordToken();
        recentToken.setExpiryDateInMilliseconds(new Timestamp(lastTokenExpiryTime));
        verifiedUser.setResetPasswordTokens(new ArrayList<>(List.of(recentToken)));

        assertThrows(PasswordResetCooldown.class, () -> rptService.tryToCreateRPT(verifiedUser));
        verify(rtpRepository, never()).save(any());
    }

    /**
     * Tests successful token creation when the previous token has expired and the cooldown has passed.
     */
    @Test
    void tryToCreateRPT_CooldownExpired_CreatesNewToken() {
        long expiredCreationTime = System.currentTimeMillis() - (TOKEN_EXPIRY_MSEC + 60000L);
        long expiredExpiryTime = expiredCreationTime + TOKEN_EXPIRY_MSEC;

        ResetPasswordToken oldToken = new ResetPasswordToken();
        oldToken.setExpiryDateInMilliseconds(new Timestamp(expiredExpiryTime));
        verifiedUser.setResetPasswordTokens(new ArrayList<>(List.of(oldToken)));

        when(rtpRepository.save(any(ResetPasswordToken.class))).thenAnswer(invocation -> {
            ResetPasswordToken savedToken = invocation.getArgument(0);
            savedToken.setId(12L);
            savedToken.setToken(UUID.randomUUID().toString());
            return savedToken;
        });

        ResetPasswordToken newToken = assertDoesNotThrow(() -> rptService.tryToCreateRPT(verifiedUser));

        assertNotNull(newToken);
        assertEquals(2, verifiedUser.getResetPasswordTokens().size());
        verify(rtpRepository, times(1)).save(any(ResetPasswordToken.class));
    }


    // ===========================================
    // TEST: verifyRPT(String tokenInput)
    // ===========================================

    /**
     * Tests that {@code verifyRPT} returns false for invalid token formats.
     */
    @Test
    void verifyRPT_InvalidFormat_ReturnsFalse() {
        assertFalse(rptService.verifyRPT(null));
        assertFalse(rptService.verifyRPT(INVALID_TOKEN_STRING));
    }

    /**
     * Tests that {@code verifyRPT} returns true for a valid, unused, and unexpired token.
     */
    @Test
    void verifyRPT_TokenValid_ReturnsTrue() {
        ResetPasswordToken validToken = createMockToken(verifiedUser, TOKEN_EXPIRY_MSEC, false);
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(validToken);

        assertTrue(rptService.verifyRPT(VALID_TOKEN_STRING));
    }

    /**
     * Tests that {@code verifyRPT} returns false for used or expired tokens.
     */
    @Test
    void verifyRPT_TokenUsedOrExpired_ReturnsFalse() {
        ResetPasswordToken usedToken = createMockToken(verifiedUser, TOKEN_EXPIRY_MSEC, true);
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(usedToken);
        assertFalse(rptService.verifyRPT(VALID_TOKEN_STRING));

        ResetPasswordToken expiredToken = createMockToken(verifiedUser, -1000L, false);
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(expiredToken);
        assertFalse(rptService.verifyRPT(VALID_TOKEN_STRING));
    }


    // ===========================================
    // TEST: getTokenByString(String token)
    // ===========================================

    /**
     * Tests that {@code getTokenByString} returns the token wrapped in an Optional when found.
     */
    @Test
    void getTokenByString_TokenFound_ReturnsOptionalOfToken() {
        ResetPasswordToken token = new ResetPasswordToken();
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(token);

        Optional<ResetPasswordToken> result = rptService.getTokenByString(VALID_TOKEN_STRING);

        assertTrue(result.isPresent());
        assertEquals(token, result.get());
    }

    /**
     * Tests that {@code getTokenByString} returns an empty Optional when the token is not found.
     */
    @Test
    void getTokenByString_TokenNotFound_ReturnsEmptyOptional() {
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(null);

        Optional<ResetPasswordToken> result = rptService.getTokenByString(VALID_TOKEN_STRING);

        assertTrue(result.isEmpty());
    }

    // ===========================================
    // TEST: removeToken(ResetPasswordToken token)
    // ===========================================

    /**
     * Tests that {@code removeToken} correctly deletes the token if it exists.
     */
    @Test
    void removeToken_TokenExists_RemovesToken() {
        ResetPasswordToken tokenToRemove = new ResetPasswordToken();
        tokenToRemove.setId(1L);
        when(rtpRepository.existsById(1L)).thenReturn(true);

        rptService.removeToken(tokenToRemove);

        verify(rtpRepository, times(1)).existsById(1L);
        verify(rtpRepository, times(1)).delete(tokenToRemove);
    }

    /**
     * Tests that {@code removeToken} does not attempt to delete the token if it doesn't exist in the repository.
     */
    @Test
    void removeToken_TokenDoesNotExist_DoesNotAttemptDelete() {
        ResetPasswordToken tokenToRemove = new ResetPasswordToken();
        tokenToRemove.setId(2L);
        when(rtpRepository.existsById(2L)).thenReturn(false);

        rptService.removeToken(tokenToRemove);

        verify(rtpRepository, times(1)).existsById(2L);
        verify(rtpRepository, never()).delete(tokenToRemove);
    }
}