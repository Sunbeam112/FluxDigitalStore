package com.artemhontar.fluxdigitalstore.service.User;

import com.artemhontar.fluxdigitalstore.exception.EmailsNotVerifiedException;
import com.artemhontar.fluxdigitalstore.exception.PasswordResetCooldown;
import com.artemhontar.fluxdigitalstore.model.LocalUser;
import com.artemhontar.fluxdigitalstore.model.ResetPasswordToken;
import com.artemhontar.fluxdigitalstore.repo.RPTRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RPTService {
    @Value("${rpt.expiry_in_msec}")
    private int tokenExpiryInMillisecond;
    private final RPTRepository rtpRepository;

    /**
     * Constructs an RPTService with the given ResetPasswordTokenRepository.
     *
     * @param rtpRepository The repository for managing ResetPasswordToken entities.
     */
    public RPTService(RPTRepository rtpRepository) {
        this.rtpRepository = rtpRepository;
    }

    /**
     * Verifies a password reset token string and returns the token object if valid.
     * This method performs comprehensive checks:
     * 1. Token format validation.
     * 2. Token existence in the repository.
     * 3. Token not being already used.
     * 4. Token not being expired.
     * 5. Associated user's email being verified.
     *
     * @param tokenInput The string representation of the token to verify.
     * @return An {@link Optional} containing the valid {@link ResetPasswordToken},
     * or an empty Optional if the token is invalid for any reason.
     */
    public Optional<ResetPasswordToken> verifyAndGetRPT(String tokenInput) {
        if (tokenInput == null || tokenInput.length() != 36) {
            return Optional.empty();
        }
        Optional<ResetPasswordToken> opToken = getTokenByString(tokenInput);
        if (opToken.isPresent()) {
            ResetPasswordToken token = opToken.get();
            if (token.getIsTokenUsed()) {
                return Optional.empty();
            }
            if (!isRTPNotExpired(token)) {
                return Optional.empty();
            }
            LocalUser user = token.getLocalUser();
            if (user == null || !user.isEmailVerified()) {
                return Optional.empty();
            }
            return Optional.of(token);
        }
        return Optional.empty();
    }

    /**
     * Marks a {@link ResetPasswordToken} as used.
     * This should be called after a successful password reset to prevent token reuse.
     * The token's 'isTokenUsed' flag is set to true and the token is updated in the repository.
     *
     * @param token The {@link ResetPasswordToken} to mark as used.
     */
    public void markTokenAsUsed(ResetPasswordToken token) {
        if (token != null) {
            token.setIsTokenUsed(true);
            rtpRepository.save(token);
        }
    }


    /**
     * Attempts to create a new Reset Password Token (RPT) for a given user.
     * This method performs checks for user validity, email verification status,
     * and a cooldown period since the last password reset request.
     *
     * @param user The {@link LocalUser} for whom to create the RPT.
     * @return The newly generated {@link ResetPasswordToken}.
     * @throws IllegalArgumentException   If the provided user is null, or if an unexpected
     *                                    state is encountered (e.g., an invalid last token).
     * @throws EmailsNotVerifiedException If the user's email is not verified.
     * @throws PasswordResetCooldown      If the user is still within the cooldown period
     *                                    for password reset requests.
     */
    public ResetPasswordToken tryToCreateRPT(LocalUser user) throws PasswordResetCooldown, EmailsNotVerifiedException, IllegalArgumentException {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (!user.isEmailVerified()) {
            throw new EmailsNotVerifiedException();
        }

        List<ResetPasswordToken> allTokens = user.getResetPasswordTokens();

        // Get the latest token based on expiry date (which is set at creation)
        Optional<ResetPasswordToken> lastTokenOpt = allTokens.stream()
                .max(Comparator.comparing(ResetPasswordToken::getExpiryDateInMilliseconds));

        if (lastTokenOpt.isPresent()) {
            ResetPasswordToken lastToken = lastTokenOpt.get();
            long timeSinceLastTokenCreation = System.currentTimeMillis() -
                    (lastToken.getExpiryDateInMilliseconds().getTime() - tokenExpiryInMillisecond);

            if (timeSinceLastTokenCreation < tokenExpiryInMillisecond) {
                throw new PasswordResetCooldown();
            }
        }

        // If no tokens or cooldown has passed, generate a new one
        ResetPasswordToken rtp = generateRPT(user);
        user.getResetPasswordTokens().add(rtp);
        return rtp;
    }

    /**
     * Verifies the validity of a given password reset token string.
     * This includes checking its format, existence, expiry status, usage status,
     * and the associated user's email verification.
     *
     * @param tokenInput The string representation of the token to verify.
     * @return true if the token is valid for a password reset, false otherwise.
     */
    public boolean verifyRPT(String tokenInput) {
        if (tokenInput == null || tokenInput.length() != 36) {
            return false;
        }
        Optional<ResetPasswordToken> opToken = getTokenByString(tokenInput);
        if (opToken.isPresent()) {
            ResetPasswordToken token = opToken.get();
            if (token.getIsTokenUsed() || !isRTPNotExpired(token)) {
                return false;
            }
            LocalUser user = opToken.get().getLocalUser();
            return user != null && user.isEmailVerified();
        }


        return false;
    }

    /**
     * Generates a new unique Reset Password Token (RPT) for a specified user.
     * The token's expiry date is set based on the configured `tokenExpiryInMillisecond`.
     * The generated token is persisted in the repository.
     *
     * @param user The {@link LocalUser} for whom to generate the RPT.
     * @return The newly created and saved {@link ResetPasswordToken}.
     */
    public ResetPasswordToken generateRPT(LocalUser user) {
        ResetPasswordToken rpt = new ResetPasswordToken();
        rpt.setExpiryDateInMilliseconds(new Timestamp(System.currentTimeMillis() + tokenExpiryInMillisecond));
        rpt.setLocalUser(user);
        rpt.setToken(UUID.randomUUID().toString());
        rtpRepository.save(rpt);
        return rpt;
    }

    private boolean isRTPNotExpired(ResetPasswordToken resetPasswordToken) {
        if (resetPasswordToken == null || resetPasswordToken.getExpiryDateInMilliseconds() == null) {
            return false;
        }
        return resetPasswordToken.getExpiryDateInMilliseconds().after(new Timestamp(System.currentTimeMillis()));
    }


    /**
     * Retrieves a Reset Password Token (RPT) from the repository by its string representation (token value).
     * The search is case-insensitive.
     *
     * @param token The string value of the token to search for.
     * @return An {@link Optional} containing the found {@link ResetPasswordToken}, or an empty Optional if not found.
     * @throws IllegalArgumentException If the provided token string is null (though the method doesn't explicitly check).
     */
    public Optional<ResetPasswordToken> getTokenByString(String token) throws IllegalArgumentException {
        return Optional.ofNullable(rtpRepository.getByTokenIgnoreCase(token));
    }


    /**
     * Removes a specific Reset Password Token (RPT) from the repository.
     * The token is only deleted if it exists in the repository.
     *
     * @param token The {@link ResetPasswordToken} to be removed.
     */
    public void removeToken(ResetPasswordToken token) {
        if (rtpRepository.existsById(token.getId()))
            rtpRepository.delete(token);
    }


    /**
     * Checks for time passed since the last password reset request.
     * This version takes the last token directly.
     *
     * @param lastToken The most recent {@link ResetPasswordToken} for the user.
     * @return int number representing the cooldown status:
     * -1 for exception cases (e.g., if the token or its expiry date is null),
     * 0 if the cooldown is still active,
     * 1 if the cooldown has passed and a new token can be created.
     */
    private int checkCooldown(ResetPasswordToken lastToken) {
        if (lastToken == null || lastToken.getExpiryDateInMilliseconds() == null) {
            return 1;
        }
        long currentTimeInMsec = System.currentTimeMillis();
        long lastTokenTimeInMsec = lastToken.getExpiryDateInMilliseconds().getTime();
        long timeElapsed = currentTimeInMsec - lastTokenTimeInMsec;
        if (timeElapsed >= tokenExpiryInMillisecond) {
            return 1;
        }
        return 0;
    }
}
