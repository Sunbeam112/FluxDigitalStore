package com.artemhontar.fluxdigitalstore.controller;

import com.artemhontar.fluxdigitalstore.api.model.User.LoginRequest;
import com.artemhontar.fluxdigitalstore.api.model.User.LoginResponse;
import com.artemhontar.fluxdigitalstore.api.model.User.PasswordResetRequest;
import com.artemhontar.fluxdigitalstore.api.model.User.RegistrationRequest;
import com.artemhontar.fluxdigitalstore.exception.*;
import com.artemhontar.fluxdigitalstore.model.ResetPasswordToken;
import com.artemhontar.fluxdigitalstore.service.User.AuthenticationService;
import com.artemhontar.fluxdigitalstore.service.User.RPTService;
import com.artemhontar.fluxdigitalstore.service.ValidationErrorsParser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/auth/v1")
public class AuthenticationController {


    private final AuthenticationService authenticationService;
    private final ValidationErrorsParser validationErrorsParser;
    private final RPTService rPTService;

    public AuthenticationController(AuthenticationService authenticationService, ValidationErrorsParser validationErrorsParser, RPTService rPTService) {
        this.authenticationService = authenticationService;
        this.validationErrorsParser = validationErrorsParser;
        this.rPTService = rPTService;
    }


    @PostMapping("/register")
    /**
     * Registers a new user in the system.
     *
     * @param registrationRequest The registration registrationRequest containing user details.
     * @param result The binding result for validation errors.
     * @return A ResponseEntity indicating the outcome of the registration attempt.
     * - HttpStatus.CREATED (201) if the user is successfully registered.
     * - HttpStatus.CONFLICT (409) if a user with the provided email already exists.
     * - HttpStatus.INTERNAL_SERVER_ERROR (500) if there's an issue sending the verification email.
     * - HttpStatus.BAD_REQUEST (400) if the provided details are not valid.
     */
    public ResponseEntity<Object> registerUser
            (@Valid @RequestBody RegistrationRequest registrationRequest, BindingResult result) {
        try {
            if (result.hasErrors()) {
                throw new DetaiIsNotVerified(validationErrorsParser.parseErrorsFrom(result));
            }
            authenticationService.registerUser(registrationRequest, result);


            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (UserAlreadyExist e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } catch (EmailFailureException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (DetaiIsNotVerified e) {
            return ResponseEntity.badRequest().body(e.getErrors());
        }
    }


    /**
     * Authenticates a user and provides a JWT upon successful login.
     *
     * @param loginRequest The login body containing user credentials (email and password).
     * @return A ResponseEntity containing a LoginResponse with a JWT if successful,
     * or an error status and reason if authentication fails.
     * - HttpStatus.OK (200) with a LoginResponse containing the JWT if login is successful.
     * - HttpStatus.FORBIDDEN (403) if the user is not verified, with a reason.
     * - HttpStatus.INTERNAL_SERVER_ERROR (500) if there's an issue with email services.
     * - HttpStatus.BAD_REQUEST (400) for general authentication failures or invalid input.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        String jwt;
        try {
            jwt = authenticationService.loginUser(loginRequest);

        } catch (UserNotVerifiedException e) {
            LoginResponse response = new LoginResponse();
            response.setSuccess(false);
            String reason = "USER_NOT_VERIFIED";
            if (e.isNewEmailSent()) {
                reason += "_NEW_EMAIL_SENT";
            }
            response.setFailureReason(reason);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (EmailFailureException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if (jwt == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } else {
            LoginResponse response = new LoginResponse();
            response.setToken(jwt);
            response.setSuccess(true);
            return ResponseEntity.ok(response);
        }

    }


    /**
     * Verifies a user's email address using a provided token.
     *
     * @param token The verification token sent to the user's email.
     * @return A ResponseEntity indicating the outcome of the verification.
     * - HttpStatus.OK (200) if the user is successfully verified.
     * - HttpStatus.CONFLICT (409) if the token is invalid or expired, or the user cannot be verified.
     */
    @PostMapping("/verify")
    public ResponseEntity<LoginResponse> verifyUser(@RequestParam String token) {
        if (authenticationService.verifyUser(token)) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.CONFLICT);
    }


    /**
     * Initiates the forgotten password process by sending a reset password email to the provided email address.
     *
     * @param email The email address of the user who forgot their password.
     * @return A ResponseEntity indicating the outcome of the request.
     * - HttpStatus.OK (200) if the reset password email is successfully sent.
     * - HttpStatus.BAD_REQUEST (400) if the email is not provided.
     * - HttpStatus.NOT_FOUND (404) if no user exists with the provided email.
     * - HttpStatus.CONFLICT (409) if the email is not verified or a password reset is on cooldown.
     * - HttpStatus.INTERNAL_SERVER_ERROR (500) if there's an issue sending the email.
     */
    @PostMapping("/forgot_password")
    public ResponseEntity<Object> forgotPassword(@RequestParam @NotBlank String email) {
        if (!authenticationService.isUserExistsByEmail(email)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            authenticationService.trySendResetPasswordEmail(email);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EmailsNotVerifiedException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("EMAIL_NOT_VERIFIED");
        } catch (EmailFailureException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (PasswordResetCooldown e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("PASSWORD_RESET_COOLDOWN");
        }
    }


    /**
     * Resets the user's password using a valid reset password token.
     *
     * @param resetBody The request body containing the reset token and the new password.
     * @param result    The binding result for validation errors of the new password.
     * @return A ResponseEntity indicating the outcome of the password reset attempt.
     * - HttpStatus.OK (200) if the password is successfully changed.
     * - HttpStatus.BAD_REQUEST (400) if the token is invalid or expired, or if the new password fails validation.
     * - HttpStatus.CONFLICT (409) if the user's email is not verified.
     * - HttpStatus.INTERNAL_SERVER_ERROR (500) if an unexpected error occurs during the password change.
     */
    @PostMapping("/reset_password")
    public ResponseEntity<Object> changeUserPassword(@Valid @RequestBody PasswordResetRequest resetBody, BindingResult result) {
        Optional<ResetPasswordToken> opToken = rPTService.verifyAndGetRPT(resetBody.getToken());

        if (opToken.isPresent()) {
            ResetPasswordToken token = opToken.get();
            try {

                boolean isPasswordChanged = authenticationService.setUserPasswordByEmail(
                        token.getLocalUser().getEmail(), resetBody.getNewPassword(), result);

                if (result.hasErrors()) {

                    throw new DetaiIsNotVerified(validationErrorsParser.parseErrorsFrom(result));
                }

                if (isPasswordChanged) {

                    rPTService.markTokenAsUsed(token);
                    return new ResponseEntity<>(HttpStatus.OK);
                } else {

                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("PASSWORD_CHANGE_FAILED");
                }

            } catch (EmailsNotVerifiedException e) {

                return ResponseEntity.status(HttpStatus.CONFLICT).body("EMAIL_NOT_VERIFIED");
            } catch (DetaiIsNotVerified e) {
                return ResponseEntity.badRequest().body(e.getErrors());
            } catch (Exception e) {
                System.err.println("Error during password reset: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("AN_UNEXPECTED_ERROR_OCCURRED");
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("INVALID_OR_EXPIRED_TOKEN");
        }
    }
}
