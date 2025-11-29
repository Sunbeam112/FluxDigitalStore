package com.artemhontar.fluxdigitalstore.service.User;

import com.artemhontar.fluxdigitalstore.api.model.User.LoginRequest;
import com.artemhontar.fluxdigitalstore.api.model.User.PasswordResetRequest;
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
import com.artemhontar.fluxdigitalstore.service.ValidationErrorsParser;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j; // Required import for logging
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * Service class responsible for core user authentication and management features,
 * including registration, login, email verification, password management, and user lookups.
 * This service leverages Spring Security components for password handling and context management.
 */
@Service
@Slf4j
public class AuthenticationService {
    //5 minutes
    private static final int COOLDOWN_IN_MS = 300000;

    private final UserRepo userRepository;
    private final JWTUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final verificationTokenRepository verificationTokenRepository;
    private final EmailVerificationService emailVerificationService;
    private final RPTService rptService;
    private final ResetPasswordEmailService resetPasswordEmailService;
    private final VerificationTokenService verificationTokenService;
    private final ValidationErrorsParser validationErrorsParser;

    /**
     * Constructs the AuthenticationService with all necessary dependencies.
     *
     * @param userRepository              The repository for managing {@link LocalUser} entities.
     * @param jwtUtils                    The utility for generating JWT tokens.
     * @param passwordEncoder             The Spring Security component for password hashing and verification.
     * @param verificationTokenRepository The repository for managing {@link VerificationToken} entities.
     * @param emailVerificationService    The service for sending email verification messages.
     * @param rptService                  The service for managing {@link ResetPasswordToken} entities.
     * @param resetPasswordEmailService   The service for sending reset password emails.
     * @param verificationTokenService    The service for creating verification tokens.
     */
    public AuthenticationService(UserRepo userRepository, JWTUtils jwtUtils, PasswordEncoder passwordEncoder, verificationTokenRepository verificationTokenRepository, EmailVerificationService emailVerificationService, RPTService rptService, ResetPasswordEmailService resetPasswordEmailService, VerificationTokenService verificationTokenService, ValidationErrorsParser validationErrorsParser) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailVerificationService = emailVerificationService;
        this.validationErrorsParser = validationErrorsParser;
        this.rptService = rptService;
        this.resetPasswordEmailService = resetPasswordEmailService;
        this.verificationTokenService = verificationTokenService;
    }


    /**
     * Authenticates a user based on email and password.
     * If authentication is successful, it returns a JWT token.
     * If the user is found but not verified, it may resend a verification email based on a cooldown period.
     *
     * @param loginRequest The DTO containing the user's email and password.
     * @return A JWT authentication token string upon successful login and verification.
     * @throws UserNotVerifiedException If the user exists but their email is not verified. Includes a flag if a new email was sent.
     * @throws EmailFailureException    If there is an issue sending the verification email.
     * @throws RuntimeException         If the user/password combination is invalid (handled by returning null).
     */
    public String loginUser(@Valid LoginRequest loginRequest) throws UserNotVerifiedException, EmailFailureException {
        log.info("Attempting to log in user with email: {}", loginRequest.getEmail());
        Optional<LocalUser> opUser = userRepository.findByEmailIgnoreCase(loginRequest.getEmail());

        if (opUser.isPresent()) {
            LocalUser user = opUser.get();
            log.debug("User found, attempting password match for email: {}", loginRequest.getEmail());

            if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                if (user.isEmailVerified()) {
                    log.info("User {} logged in successfully. Token generated.", user.getEmail());
                    return jwtUtils.generateToken(user.getUsername());
                } else {
                    List<VerificationToken> verificationTokens = user.getVerificationTokens();
                    boolean resend = verificationTokens.isEmpty() ||
                            verificationTokens.getFirst()
                                    .getCreatedTimestamp().before(new Timestamp(System.currentTimeMillis() - COOLDOWN_IN_MS));
                    if (resend) {
                        log.info("User {} is not verified. Resending verification email.", user.getEmail());
                        VerificationToken verificationToken = verificationTokenService.createVerificationToken(user);
                        verificationTokenRepository.save(verificationToken);
                        emailVerificationService.sendEmailConformationMessage(verificationToken);
                    } else {
                        log.warn("User {} is not verified, but is still in cooldown period. Not resending email.", user.getEmail());
                    }
                    throw new UserNotVerifiedException(resend);
                }
            } else {
                log.warn("Login failed for user {}: Invalid password.", loginRequest.getEmail());
            }
        } else {
            log.warn("Login failed: User not found for email: {}", loginRequest.getEmail());
        }
        return null;
    }


    /**
     * Registers a new user, hashes the password, and initiates the email verification process.
     *
     * @param registrationRequest The DTO containing the user's registration details (email, password).
     * @param bindingResult       The result object for validation errors (though not actively used for throwing exceptions).
     * @throws UserAlreadyExist      If a user with the provided email already exists.
     * @throws EmailFailureException If there is an issue sending the initial verification email.
     */
    @Transactional
    public void registerUser(@Valid @RequestBody RegistrationRequest registrationRequest, BindingResult bindingResult) throws UserAlreadyExist, EmailFailureException {
        log.info("Attempting to register user with email: {}", registrationRequest.getEmail());
        Optional<LocalUser> opUser = userRepository.findByEmailIgnoreCase(registrationRequest.getEmail());
        if (opUser.isPresent()) {
            log.warn("Registration failed: User already exists with email: {}", registrationRequest.getEmail());
            throw new UserAlreadyExist();
        }

        LocalUser user = new LocalUser();

        user.setEmail(registrationRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        userRepository.save(user);
        log.info("New user saved to DB with email: {}", user.getEmail());

        VerificationToken verificationToken = verificationTokenService.createVerificationToken(user);
        verificationTokenRepository.save(verificationToken);

        log.debug("Verification token created for user: {}", user.getEmail());
        emailVerificationService.sendEmailConformationMessage(verificationToken);
        log.info("Verification email sent to user: {}", user.getEmail());
    }


    /**
     * Processes an email verification token. If the token is valid, the associated user's
     * email verification status is updated to true, and the token is deleted.
     *
     * @param token The unique verification token received from the email link.
     * @return true if the user's email was successfully verified or was already verified, false otherwise.
     */
    @Transactional
    public boolean verifyUser(String token) {
        log.info("Attempting to verify user with token: {}", token);
        Optional<VerificationToken> opToken = verificationTokenRepository.findByToken(token);
        if (opToken.isPresent()) {
            VerificationToken verificationToken = opToken.get();
            LocalUser user = verificationToken.getLocalUser();
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
                userRepository.save(user);
                verificationTokenRepository.deleteByLocalUser(user);
                log.info("Email verified successfully for user ID: {}", user.getId());
                return true;
            } else {
                log.info("User ID {} is already verified.", user.getId());
            }
        } else {
            log.warn("Verification failed: Token not found: {}", token);
        }
        return false;
    }


    /**
     * Sets a new password for an existing user, provided the user's email is already verified.
     * This method is typically called after a successful password reset token verification.
     *
     * @param email    The email of the user whose password is to be changed.
     * @param password The new password, which must adhere to specified size constraints.
     * @param result   The binding result for password validation.
     * @return true if the password was successfully set, false if validation errors occurred.
     * @throws EmailsNotVerifiedException If the user exists but their email is not verified.
     */
    @Transactional
    public boolean setUserPasswordByEmail(String email,
                                          @NotNull @NotBlank @Size(min = 8, max = 64) String password,
                                          BindingResult result) throws EmailsNotVerifiedException {
        log.info("Attempting to set new password for email: {}", email);
        if (result.hasErrors()) {
            log.warn("Set password failed for {}: Validation errors present.", email);
            return false;
        }
        Optional<LocalUser> opUser = userRepository.findByEmailIgnoreCase(email);
        if (opUser.isPresent()) {
            LocalUser user = opUser.get();
            if (user.isEmailVerified()) {
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
                log.info("Password successfully updated for verified user: {}", email);
                return true;
            } else {
                log.warn("Set password failed for {}: Email is not verified.", email);
                throw new EmailsNotVerifiedException();
            }
        }
        log.warn("Set password failed: User not found for email: {}", email);
        return false;
    }

    /**
     * Attempts to send a reset password email to a user.
     * This action is gated by checking if the user is verified and by a reset password cooldown period.
     *
     * @param email The email address to send the reset link to.
     * @throws EmailsNotVerifiedException If the user's email is not verified.
     * @throws EmailFailureException      If the email sending process fails.
     * @throws PasswordResetCooldown      If a reset password email was recently sent to this user.
     */
    public void trySendResetPasswordEmail(String email) throws EmailsNotVerifiedException, EmailFailureException, PasswordResetCooldown {
        log.info("Attempting to send password reset email for: {}", email);
        Optional<LocalUser> opUser = userRepository.findByEmailIgnoreCase(email);
        if (opUser.isPresent()) {
            LocalUser user = opUser.get();
            if (user.isEmailVerified()) {
                try {
                    ResetPasswordToken rpt = rptService.tryToCreateRPT(user);
                    resetPasswordEmailService.sendResetPasswordEmail(rpt);
                    log.info("Password reset email sent successfully to: {}", email);
                } catch (EmailFailureException ex) {
                    log.error("Failed to send password reset email to {}.", email, ex);
                    throw new EmailFailureException(ex.getMessage());
                } catch (PasswordResetCooldown ex) {
                    log.warn("Password reset attempt for {} rejected: Cooldown period active.", email);
                    throw new PasswordResetCooldown();
                }

            } else {
                log.warn("Password reset attempt for {} rejected: Email is not verified.", email);
                throw new EmailsNotVerifiedException();
            }
        } else {
            log.info("Password reset request for non-existent email: {}. Silently failing.", email);
        }

    }

    /**
     * Attempts to retrieve the currently authenticated {@link LocalUser} from the Spring Security context.
     *
     * @return An Optional containing the authenticated {@link LocalUser} if one is found, otherwise an empty Optional.
     */
    public Optional<LocalUser> tryGetCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal == null) return Optional.empty();
        String email = (String) principal;
        if (email.trim().isEmpty()) return Optional.empty();
        return userRepository.findByEmailIgnoreCase(email);
    }

    /**
     * Checks if a user exists in the database by their email address.
     *
     * @param email The email address to check.
     * @return true if a user with the given email exists, false otherwise.
     */
    public boolean isUserExistsByEmail(String email) {
        // Logging only at debug level for simple existence checks
        boolean exists = userRepository.findByEmailIgnoreCase(email).isPresent();
        log.debug("Checking existence of user by email {}: {}", email, exists);
        return exists;
    }

    /**
     * Checks if a user exists in the database by their unique ID.
     *
     * @param id The ID to check.
     * @return true if a user with the given ID exists, false otherwise.
     */
    public boolean isUserExistsByID(Long id) {
        boolean exists = userRepository.findById(id).isPresent();
        log.debug("Checking existence of user by ID {}: {}", id, exists);
        return exists;
    }


    /**
     * Checks if a user's email has been verified.
     *
     * @param email The email address of the user.
     * @return true if the user exists and their email is verified, false otherwise.
     */
    public boolean isUserEmailVerified(String email) {
        Optional<LocalUser> opUser = userRepository.findByEmailIgnoreCase(email);
        boolean verified = opUser.map(LocalUser::isEmailVerified).orElse(false);
        log.debug("Verification status for {}: {}", email, verified);
        return verified;
    }

    /**
     * Retrieves a user entity by its ID.
     *
     * @param id The ID of the user to retrieve.
     * @return An Optional containing the {@link LocalUser} if found, otherwise an empty Optional.
     */
    public Optional<LocalUser> getUserByID(Long id) {
        log.debug("Fetching user by ID: {}", id);
        return userRepository.findById(id);
    }

    /**
     * Retrieves a user entity by its email address.
     *
     * @param email The email of the user to retrieve.
     * @return An Optional containing the {@link LocalUser} if found, otherwise an empty Optional.
     */
    public Optional<LocalUser> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmailIgnoreCase(email);
    }


    /**
     * Checks if the currently authenticated user possesses the 'ADMIN' authority/role.
     * This is used for privilege escalation and security bypass checks.
     *
     * @return true if the user has the 'ROLE_ADMIN' authority, false otherwise.
     */
    public boolean isAdmin() {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        log.debug("Current user is Admin: {}", isAdmin);
        return isAdmin;
    }


    /**
     * Handles the complete logic for resetting a user's password using a valid reset password token.
     *
     * @param resetBody The request body containing the reset token and the new password.
     * @param result    The binding result for validation errors of the new password.
     * @throws InvalidTokenException         if the token is invalid, expired, or used.
     * @throws EmailsNotVerifiedException    if the user associated with the token is not verified.
     * @throws DetaiIsNotVerified            if the new password fails validation (validation errors in result).
     * @throws PasswordChangeFailedException if the password change operation fails unexpectedly (e.g., DB update failure).
     */
    @Transactional
    public void resetUserPassword(PasswordResetRequest resetBody, BindingResult result)
            throws InvalidTokenException, EmailsNotVerifiedException, DetaiIsNotVerified, PasswordChangeFailedException {

        log.info("Starting password reset process for token: {}", resetBody.getToken());
        // 1. Verify and retrieve the Reset Password Token (RPT)
        Optional<ResetPasswordToken> opToken = rptService.verifyAndGetRPT(resetBody.getToken());

        if (opToken.isEmpty()) {
            log.warn("Password reset failed: Invalid or expired token provided.");
            throw new InvalidTokenException("INVALID_OR_EXPIRED_TOKEN");
        }

        ResetPasswordToken token = opToken.get();
        String userEmail = token.getLocalUser().getEmail();
        String newPassword = resetBody.getNewPassword();

        log.debug("Token verified. Attempting to set new password for user: {}", userEmail);

        // 2. Set the new password. This method also handles the password validation via BindingResult.
        boolean isPasswordChanged;
        try {
            isPasswordChanged = setUserPasswordByEmail(userEmail, newPassword, result);
        } catch (EmailsNotVerifiedException e) {
            // While rPTService.verifyAndGetRPT checks for verification, this handles a race condition or logic flaw.
            log.error("Password reset failed: User {} is not verified.", userEmail);
            throw e;
        }

        // 3. Handle password validation errors
        if (result.hasErrors()) {
            log.warn("Password reset failed for {}: New password failed validation.", userEmail);
            throw new DetaiIsNotVerified(validationErrorsParser.parseErrorsFrom(result));
        }

        // 4. Handle success or generic failure
        if (isPasswordChanged) {
            // Mark the token as used only upon successful password change
            rptService.markTokenAsUsed(token);
            log.info("Password successfully reset and token marked as used for user: {}", userEmail);
        } else {
            // isPasswordChanged returns false only if setUserPasswordByEmail fails for a reason
            // other than validation or email verification (which is usually a DB/internal error).
            log.error("Password change failed for user {}: Internal update failure.", userEmail);
            throw new PasswordChangeFailedException("PASSWORD_CHANGE_FAILED");
        }
    }
}