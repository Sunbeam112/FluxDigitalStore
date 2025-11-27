package com.artemhontar.fluxdigitalstore.service;

import com.artemhontar.fluxdigitalstore.exception.EmailsNotVerifiedException;
import com.artemhontar.fluxdigitalstore.exception.PasswordResetCooldown;
import com.artemhontar.fluxdigitalstore.model.LocalUser;
import com.artemhontar.fluxdigitalstore.model.ResetPasswordToken;
import com.artemhontar.fluxdigitalstore.repo.RPTRepository;
import com.artemhontar.fluxdigitalstore.service.User.RPTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList; // Added import
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RPTServiceTest {

    @Mock
    private RPTRepository rtpRepository;

    @InjectMocks
    private RPTService rptService;

    // --- Constants and Mocks ---
    private static final long TOKEN_EXPIRY_MSEC = 300000L; // 5 minutes
    private static final String VALID_TOKEN_STRING = UUID.randomUUID().toString(); // 36 chars
    private static final String INVALID_TOKEN_STRING = "short";
    private static final Long USER_ID = 1L;

    private LocalUser verifiedUser;
    private LocalUser unverifiedUser;

    @BeforeEach
    void setUp() throws Exception {
        // Use reflection to manually set the @Value property
        Field field = RPTService.class.getDeclaredField("tokenExpiryInMillisecond");
        field.setAccessible(true);
        field.set(rptService, (int) TOKEN_EXPIRY_MSEC);

        // Setup verified user
        verifiedUser = new LocalUser();
        verifiedUser.setId(USER_ID);
        verifiedUser.setEmailVerified(true);
        verifiedUser.setResetPasswordTokens(new ArrayList<>());

        // Setup unverified user
        unverifiedUser = new LocalUser();
        unverifiedUser.setId(2L);
        unverifiedUser.setEmailVerified(false);
        unverifiedUser.setResetPasswordTokens(new ArrayList<>());
    }

    // Helper method to create a token for a given user and expiry offset
    private ResetPasswordToken createMockToken(LocalUser user, long timeOffsetMsec, boolean isUsed) {
        ResetPasswordToken token = new ResetPasswordToken();
        token.setId(10L);
        token.setToken(VALID_TOKEN_STRING);
        token.setLocalUser(user);
        token.setIsTokenUsed(isUsed);
        // Expiry date = Current Time + Offset.
        // A future offset means not expired, a past offset means expired.
        token.setExpiryDateInMilliseconds(new Timestamp(System.currentTimeMillis() + timeOffsetMsec));
        return token;
    }

    // ===========================================
    // TEST: verifyAndGetRPT(String tokenInput)
    // ===========================================

    @Test
    void verifyAndGetRPT_InvalidFormat_ReturnsEmpty() {
        // Act & Assert
        assertTrue(rptService.verifyAndGetRPT(null).isEmpty());
        assertTrue(rptService.verifyAndGetRPT(INVALID_TOKEN_STRING).isEmpty());
        verify(rtpRepository, never()).getByTokenIgnoreCase(anyString());
    }

    @Test
    void verifyAndGetRPT_TokenNotFound_ReturnsEmpty() {
        // Arrange
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(null);

        // Act & Assert
        assertTrue(rptService.verifyAndGetRPT(VALID_TOKEN_STRING).isEmpty());
    }

    @Test
    void verifyAndGetRPT_TokenUsed_ReturnsEmpty() {
        // Arrange
        ResetPasswordToken usedToken = createMockToken(verifiedUser, TOKEN_EXPIRY_MSEC, true);
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(usedToken);

        // Act & Assert
        assertTrue(rptService.verifyAndGetRPT(VALID_TOKEN_STRING).isEmpty());
    }

    @Test
    void verifyAndGetRPT_TokenExpired_ReturnsEmpty() {
        // Arrange
        // Token expired 1 second ago
        ResetPasswordToken expiredToken = createMockToken(verifiedUser, -1000L, false);
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(expiredToken);

        // Act & Assert
        assertTrue(rptService.verifyAndGetRPT(VALID_TOKEN_STRING).isEmpty());
    }

    @Test
    void verifyAndGetRPT_UserEmailNotVerified_ReturnsEmpty() {
        // Arrange
        ResetPasswordToken token = createMockToken(unverifiedUser, TOKEN_EXPIRY_MSEC, false);
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(token);

        // Act & Assert
        assertTrue(rptService.verifyAndGetRPT(VALID_TOKEN_STRING).isEmpty());
    }

    @Test
    void verifyAndGetRPT_TokenValid_ReturnsToken() {
        // Arrange
        ResetPasswordToken validToken = createMockToken(verifiedUser, TOKEN_EXPIRY_MSEC, false);
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(validToken);

        // Act
        Optional<ResetPasswordToken> result = rptService.verifyAndGetRPT(VALID_TOKEN_STRING);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(validToken, result.get());
    }

    // ===========================================
    // TEST: markTokenAsUsed(ResetPasswordToken token)
    // ===========================================

    @Test
    void markTokenAsUsed_ValidToken_MarksAndSaves() {
        // Arrange
        ResetPasswordToken token = createMockToken(verifiedUser, TOKEN_EXPIRY_MSEC, false);

        // Act
        rptService.markTokenAsUsed(token);

        // Assert
        assertTrue(token.getIsTokenUsed());
        verify(rtpRepository, times(1)).save(token);
    }

    @Test
    void markTokenAsUsed_NullToken_DoesNothing() {
        // Act
        rptService.markTokenAsUsed(null);

        // Assert
        verify(rtpRepository, never()).save(any());
    }

    // ===========================================
    // TEST: tryToCreateRPT(LocalUser user)
    // ===========================================

    @Test
    void tryToCreateRPT_NullUser_ThrowsIllegalArgumentException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> rptService.tryToCreateRPT(null));
        verify(rtpRepository, never()).save(any());
    }

    @Test
    void tryToCreateRPT_EmailNotVerified_ThrowsEmailsNotVerifiedException() {
        // Act & Assert
        assertThrows(EmailsNotVerifiedException.class, () -> rptService.tryToCreateRPT(unverifiedUser));
        verify(rtpRepository, never()).save(any());
    }

    @Test
    void tryToCreateRPT_NoPreviousTokens_CreatesNewToken() {
        // Arrange
        // Mock the save operation to capture the generated token
        when(rtpRepository.save(any(ResetPasswordToken.class))).thenAnswer(invocation -> {
            ResetPasswordToken savedToken = invocation.getArgument(0);
            savedToken.setId(11L); // Simulate ID generation
            savedToken.setToken(UUID.randomUUID().toString()); // Simulate UUID generation
            return savedToken;
        });

        // Act
        ResetPasswordToken newToken = assertDoesNotThrow(() -> rptService.tryToCreateRPT(verifiedUser));

        // Assert
        assertNotNull(newToken);
        assertFalse(verifiedUser.getResetPasswordTokens().isEmpty());
        assertTrue(newToken.getExpiryDateInMilliseconds().after(new Timestamp(System.currentTimeMillis())));
        verify(rtpRepository, times(1)).save(any(ResetPasswordToken.class));
    }

    @Test
    void tryToCreateRPT_CooldownActive_ThrowsPasswordResetCooldown() {
        // Arrange
        // Create a token that was created very recently (within 5 minutes, i.e., 1 minute ago)
        long lastTokenCreationTime = System.currentTimeMillis() - 60000L; // 1 minute ago
        long lastTokenExpiryTime = lastTokenCreationTime + TOKEN_EXPIRY_MSEC;

        ResetPasswordToken recentToken = new ResetPasswordToken();
        recentToken.setExpiryDateInMilliseconds(new Timestamp(lastTokenExpiryTime));
        // FIX: Ensure the list is mutable to prevent UnsupportedOperationException when the service tries to add a new token
        verifiedUser.setResetPasswordTokens(new ArrayList<>(List.of(recentToken)));

        // Act & Assert
        assertThrows(PasswordResetCooldown.class, () -> rptService.tryToCreateRPT(verifiedUser));
        verify(rtpRepository, never()).save(any());
    }

    @Test
    void tryToCreateRPT_CooldownExpired_CreatesNewToken() {
        // Arrange
        // Create a token that was created well outside the cooldown window (e.g., 6 minutes ago)
        long expiredCreationTime = System.currentTimeMillis() - (TOKEN_EXPIRY_MSEC + 60000L); // 6 minutes ago
        long expiredExpiryTime = expiredCreationTime + TOKEN_EXPIRY_MSEC;

        ResetPasswordToken oldToken = new ResetPasswordToken();
        oldToken.setExpiryDateInMilliseconds(new Timestamp(expiredExpiryTime));
        // FIX: Ensure the list is mutable to prevent UnsupportedOperationException when the service tries to add a new token
        verifiedUser.setResetPasswordTokens(new ArrayList<>(List.of(oldToken)));

        // Mock the save operation for the new token
        when(rtpRepository.save(any(ResetPasswordToken.class))).thenAnswer(invocation -> {
            ResetPasswordToken savedToken = invocation.getArgument(0);
            savedToken.setId(12L);
            savedToken.setToken(UUID.randomUUID().toString());
            return savedToken;
        });

        // Act
        ResetPasswordToken newToken = assertDoesNotThrow(() -> rptService.tryToCreateRPT(verifiedUser));

        // Assert
        assertNotNull(newToken);
        assertEquals(2, verifiedUser.getResetPasswordTokens().size()); // Old token + new token
        verify(rtpRepository, times(1)).save(any(ResetPasswordToken.class));
    }


    // ===========================================
    // TEST: verifyRPT(String tokenInput)
    // ===========================================

    @Test
    void verifyRPT_InvalidFormat_ReturnsFalse() {
        assertFalse(rptService.verifyRPT(null));
        assertFalse(rptService.verifyRPT(INVALID_TOKEN_STRING));
    }

    @Test
    void verifyRPT_TokenValid_ReturnsTrue() {
        // Arrange
        ResetPasswordToken validToken = createMockToken(verifiedUser, TOKEN_EXPIRY_MSEC, false);
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(validToken);

        // Act & Assert
        assertTrue(rptService.verifyRPT(VALID_TOKEN_STRING));
    }

    @Test
    void verifyRPT_TokenUsedOrExpired_ReturnsFalse() {
        // Arrange 1: Used Token
        ResetPasswordToken usedToken = createMockToken(verifiedUser, TOKEN_EXPIRY_MSEC, true);
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(usedToken);
        assertFalse(rptService.verifyRPT(VALID_TOKEN_STRING));

        // Arrange 2: Expired Token (Need to ensure it's a fresh mock call)
        ResetPasswordToken expiredToken = createMockToken(verifiedUser, -1000L, false);
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(expiredToken);
        assertFalse(rptService.verifyRPT(VALID_TOKEN_STRING));
    }


    // ===========================================
    // TEST: getTokenByString(String token)
    // ===========================================

    @Test
    void getTokenByString_TokenFound_ReturnsOptionalOfToken() {
        // Arrange
        ResetPasswordToken token = new ResetPasswordToken();
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(token);

        // Act
        Optional<ResetPasswordToken> result = rptService.getTokenByString(VALID_TOKEN_STRING);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(token, result.get());
    }

    @Test
    void getTokenByString_TokenNotFound_ReturnsEmptyOptional() {
        // Arrange
        when(rtpRepository.getByTokenIgnoreCase(VALID_TOKEN_STRING)).thenReturn(null);

        // Act
        Optional<ResetPasswordToken> result = rptService.getTokenByString(VALID_TOKEN_STRING);

        // Assert
        assertTrue(result.isEmpty());
    }

    // ===========================================
    // TEST: removeToken(ResetPasswordToken token)
    // ===========================================

    @Test
    void removeToken_TokenExists_RemovesToken() {
        // Arrange
        ResetPasswordToken tokenToRemove = new ResetPasswordToken();
        tokenToRemove.setId(1L);
        when(rtpRepository.existsById(1L)).thenReturn(true);

        // Act
        rptService.removeToken(tokenToRemove);

        // Assert
        verify(rtpRepository, times(1)).existsById(1L);
        verify(rtpRepository, times(1)).delete(tokenToRemove);
    }

    @Test
    void removeToken_TokenDoesNotExist_DoesNotAttemptDelete() {
        // Arrange
        ResetPasswordToken tokenToRemove = new ResetPasswordToken();
        tokenToRemove.setId(2L);
        when(rtpRepository.existsById(2L)).thenReturn(false);

        // Act
        rptService.removeToken(tokenToRemove);

        // Assert
        verify(rtpRepository, times(1)).existsById(2L);
        verify(rtpRepository, never()).delete(tokenToRemove);
    }
}