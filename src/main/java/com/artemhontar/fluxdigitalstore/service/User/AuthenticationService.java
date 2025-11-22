package com.artemhontar.fluxdigitalstore.service.User;

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
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
public class AuthenticationService {
    //5 minutes
    private static final int COOLDOWN_IN_MS = 300000;

    private final UserRepo userRepository;

    private final JWTUtils jwtUtils;
    private final EncryptionService encryptionService;
    private final verificationTokenRepository verificationTokenRepository;
    private final EmailVerificationService emailVerificationService;
    private final RPTService rptService;
    private final ResetPasswordEmailService resetPasswordEmailService;
    private final VerificationTokenService verificationTokenService;

    public AuthenticationService(UserRepo userRepository, JWTUtils jwtUtils, EncryptionService encryptionService, verificationTokenRepository verificationTokenRepository, EmailVerificationService emailVerificationService, RPTService rptService, ResetPasswordEmailService resetPasswordEmailService, VerificationTokenService verificationTokenService) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.encryptionService = encryptionService;
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailVerificationService = emailVerificationService;
        this.rptService = rptService;
        this.resetPasswordEmailService = resetPasswordEmailService;
        this.verificationTokenService = verificationTokenService;
    }


    public String loginUser(@Valid LoginRequest loginRequest) throws UserNotVerifiedException, EmailFailureException {
        Optional<LocalUser> opUser = userRepository.findByEmailIgnoreCase(loginRequest.getEmail());
        if (opUser.isPresent()) {
            LocalUser user = opUser.get();
            if (encryptionService.decryptPassword(loginRequest.getPassword(), user.getPassword())) {
                if (user.isEmailVerified()) {
                    return jwtUtils.generateToken(user.getUsername());
                } else {
                    List<VerificationToken> verificationTokens = user.getVerificationTokens();
                    boolean resend = verificationTokens.isEmpty() ||
                            verificationTokens.getFirst()
                                    .getCreatedTimestamp().before(new Timestamp(System.currentTimeMillis() - COOLDOWN_IN_MS));
                    if (resend) {
                        VerificationToken verificationToken = verificationTokenService.createVerificationToken(user);
                        verificationTokenRepository.save(verificationToken);
                        emailVerificationService.sendEmailConformationMessage(verificationToken);
                    }
                    throw new UserNotVerifiedException(resend);
                }
            }
        }
        return null;
    }


    @Transactional
    public void registerUser(@Valid @RequestBody RegistrationRequest registrationRequest, BindingResult bindingResult) throws UserAlreadyExist, EmailFailureException {
        Optional<LocalUser> opUser = userRepository.findByEmailIgnoreCase(registrationRequest.getEmail());
        if (opUser.isPresent()) {
            throw new UserAlreadyExist();
        }

        LocalUser user = new LocalUser();
        //TODO: ADD EMAIL ENCRYPTION
        user.setEmail(registrationRequest.getEmail());
        user.setPassword(encryptionService.encryptPassword(registrationRequest.getPassword()));
        userRepository.save(user);

        VerificationToken verificationToken = verificationTokenService.createVerificationToken(user);

        verificationTokenRepository.save(verificationToken);
        emailVerificationService.sendEmailConformationMessage(verificationToken);
    }


    @Transactional
    public boolean verifyUser(String token) {
        Optional<VerificationToken> opToken = verificationTokenRepository.findByToken(token);
        if (opToken.isPresent()) {
            VerificationToken verificationToken = opToken.get();
            LocalUser user = verificationToken.getLocalUser();
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
                userRepository.save(user);
                verificationTokenRepository.deleteByLocalUser(user);
                return true;
            }
        }
        return false;
    }


    @Transactional
    public boolean setUserPasswordByEmail(String email,
                                          @NotNull @NotBlank @Size(min = 8, max = 64) String password,
                                          BindingResult result) throws EmailsNotVerifiedException {
        if (result.hasErrors()) {
            return false;
        }
        Optional<LocalUser> opUser = userRepository.findByEmailIgnoreCase(email);
        if (opUser.isPresent()) {
            LocalUser user = opUser.get();
            if (user.isEmailVerified()) {
                user.setPassword(encryptionService.encryptPassword(password));
                userRepository.save(user);
                return true;
            } else {
                throw new EmailsNotVerifiedException();
            }
        }
        return false;
    }

    public void trySendResetPasswordEmail(String email) throws EmailsNotVerifiedException, EmailFailureException, PasswordResetCooldown {
        Optional<LocalUser> opUser = userRepository.findByEmailIgnoreCase(email);
        if (opUser.isPresent()) {
            LocalUser user = opUser.get();
            if (user.isEmailVerified()) {
                try {
                    ResetPasswordToken rpt = rptService.tryToCreateRPT(user);
                    resetPasswordEmailService.sendResetPasswordEmail(rpt);
                } catch (EmailFailureException ex) {
                    throw new EmailFailureException();
                } catch (PasswordResetCooldown ex) {
                    throw new PasswordResetCooldown();
                }

            } else {
                throw new EmailsNotVerifiedException();
            }
        }

    }

    public Optional<LocalUser> tryGetCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal == null) return Optional.empty();
        String email = (String) principal;
        if (email.trim().isEmpty()) return Optional.empty();
        return userRepository.findByEmailIgnoreCase(email);
    }

    public boolean isUserExistsByEmail(String email) {
        Optional<LocalUser> opUser = userRepository.findByEmailIgnoreCase(email);
        return opUser.isPresent();
    }

    public boolean isUserExistsByID(Long id) {
        Optional<LocalUser> opUser = userRepository.findById(id);
        return opUser.isPresent();
    }


    public boolean isUserEmailVerified(String email) {
        Optional<LocalUser> opUser = userRepository.findByEmailIgnoreCase(email);
        if (opUser.isPresent()) {
            LocalUser user = opUser.get();
            return user.isEmailVerified();
        }
        return false;
    }

    public Optional<LocalUser> getUserByID(Long id) {
        return userRepository.findById(id);

    }

    public Optional<LocalUser> getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }


}
