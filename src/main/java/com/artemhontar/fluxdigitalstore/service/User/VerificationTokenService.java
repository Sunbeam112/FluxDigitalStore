package com.artemhontar.fluxdigitalstore.service.User;

import com.artemhontar.fluxdigitalstore.api.security.JWTUtils;
import com.artemhontar.fluxdigitalstore.model.LocalUser;
import com.artemhontar.fluxdigitalstore.model.VerificationToken;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class VerificationTokenService {

    private final JWTUtils jWTUtils;

    public VerificationTokenService(JWTUtils jWTUtils) {
        this.jWTUtils = jWTUtils;
    }

    /**
     * Creates a new verification token and associates it with the user.
     * This method is transactional to ensure the token is correctly linked
     * to the user's collection before the outer method saves it.
     * * @param user The user for whom the token is being created.
     * @return The newly created VerificationToken.
     */
    @Transactional
    public VerificationToken createVerificationToken(LocalUser user) {
        VerificationToken token = new VerificationToken();
        token.setToken(jWTUtils.generateToken(user.getEmail()));
        token.setCreatedTimestamp(new Timestamp(System.currentTimeMillis()));
        token.setLocalUser(user);
        user.getVerificationTokens().add(token);

        return token;
    }
}
