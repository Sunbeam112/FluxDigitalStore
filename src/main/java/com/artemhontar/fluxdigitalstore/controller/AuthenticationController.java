package com.artemhontar.fluxdigitalstore.controller;

import com.artemhontar.fluxdigitalstore.api.model.User.LoginRequest;
import com.artemhontar.fluxdigitalstore.api.model.User.LoginResponse;
import com.artemhontar.fluxdigitalstore.api.model.User.RegistrationRequest;
import com.artemhontar.fluxdigitalstore.exception.DetaiIsNotVerified;
import com.artemhontar.fluxdigitalstore.exception.EmailFailureException;
import com.artemhontar.fluxdigitalstore.exception.UserAlreadyExist;
import com.artemhontar.fluxdigitalstore.exception.UserNotVerifiedException;
import com.artemhontar.fluxdigitalstore.service.UserService;
import com.artemhontar.fluxdigitalstore.service.ValidationErrorsParser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/auth/v1")
public class AuthenticationController {


    private final UserService userService;
    private final ValidationErrorsParser validationErrorsParser;

    public AuthenticationController(UserService userService, ValidationErrorsParser validationErrorsParser) {
        this.userService = userService;
        this.validationErrorsParser = validationErrorsParser;
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
            userService.registerUser(registrationRequest, result);


            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (UserAlreadyExist e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } catch (EmailFailureException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (DetaiIsNotVerified e) {
            return ResponseEntity.badRequest().body(e.getErrors());
        }
    }

    @PostMapping("/login")
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
    public ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        String jwt;
        try {
            jwt = userService.loginUser(loginRequest);

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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
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
        if (userService.verifyUser(token)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }


}
