package com.artemhontar.fluxdigitalstore.api.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JWTUtils {
    private static final String EMAIL_SUBJECT = "User Details";
    private static final String EMAIL_CLAIM = "EMAIL_CLAIM";
    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.company_name}")
    private String companyName;
    @Value("${jwt.expiry_in_sec}")
    private int expiryInSec;
    private Algorithm algorithm;

    @PostConstruct
    public void postConstruct() {
        algorithm = Algorithm.HMAC256(secret);
    }

    public String generateToken(String email) {
        return JWT.create()
                .withSubject(EMAIL_SUBJECT)
                .withClaim(EMAIL_CLAIM, email)
                .withExpiresAt(new Date(System.currentTimeMillis() + expiryInSec * 1000L))
                .withIssuedAt(new Date())
                .withIssuer(companyName)
                .sign(algorithm);
    }

    public String verifyToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(algorithm)
                .withSubject(EMAIL_SUBJECT)
                .withIssuer(companyName)
                .build();
        DecodedJWT jwt = verifier.verify(token);
        return jwt.getClaim(EMAIL_CLAIM).asString();
    }

    public String getEmailFromToken(String token) {
        DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
        return jwt.getClaim(EMAIL_CLAIM).asString();
    }

}
