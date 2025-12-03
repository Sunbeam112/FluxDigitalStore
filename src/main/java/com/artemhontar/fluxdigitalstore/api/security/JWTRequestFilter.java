package com.artemhontar.fluxdigitalstore.api.security;

import com.artemhontar.fluxdigitalstore.model.LocalUser;
import com.artemhontar.fluxdigitalstore.model.repo.UserRepo;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JWTRequestFilter extends OncePerRequestFilter {

    private final UserRepo userRepository;
    private final JWTUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public JWTRequestFilter(UserRepo userRepository, JWTUtils jwtUtils, UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String tokenHeader = request.getHeader("Authorization");
        if (tokenHeader != null && tokenHeader.startsWith("Bearer ")) {
            String token = tokenHeader.substring(7);
            if (token.isBlank()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is empty");
            } else {
                try {
                    String emailFromToken = jwtUtils.getEmailFromToken(token);
                    Optional<LocalUser> opUser = userRepository.findByEmailIgnoreCase(emailFromToken);
                    if (opUser.isPresent()) {
                        LocalUser user = opUser.get();
                        if (user.isEmailVerified()) {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(emailFromToken);
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    emailFromToken, userDetails.getPassword(), userDetails.getAuthorities());
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }
                } catch (JWTVerificationException e) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is invalid");
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
