package com.itibari.auth.service.impl;

import com.itibari.auth.model.AuthUser;
import com.itibari.auth.repository.AuthUserRepository;
import com.itibari.auth.service.GoogleLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleLoginServiceImpl implements GoogleLoginService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthUser findOrCreateUser(Jwt idToken) {
        String subject = requiredClaim(idToken, "sub");
        String email = requiredClaim(idToken, "email");

        if (!Boolean.TRUE.equals(idToken.getClaim("email_verified"))) {
            throw new BadCredentialsException("Google email is not verified");
        }

        return authUserRepository.findByGoogleSubject(subject)
                .map(user -> restoreAndUpdate(user, idToken, email))
                .or(() -> authUserRepository.findByEmailIgnoreCase(email)
                        .map(user -> linkGoogleSubject(user, idToken, subject, email)))
                .or(() -> authUserRepository.findByUsernameIgnoreCase(email)
                        .map(user -> linkGoogleSubject(user, idToken, subject, email)))
                .orElseGet(() -> authUserRepository.save(AuthUser.builder()
                        .username("google-" + subject)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .googleSubject(subject)
                        .email(email)
                        .firstName(idToken.getClaimAsString("given_name"))
                        .lastName(idToken.getClaimAsString("family_name"))
                        .enabled(true)
                        .activeStatus(true)
                        .accountNonExpired(true)
                        .credentialsNonExpired(true)
                        .accountNonLocked(true)
                        .build()));
    }

    private AuthUser linkGoogleSubject(AuthUser user, Jwt idToken, String subject, String email) {
        if (user.getGoogleSubject() != null && !user.getGoogleSubject().equals(subject)) {
            throw new BadCredentialsException("Email is already linked to another Google account");
        }
        user.setGoogleSubject(subject);
        return restoreAndUpdate(user, idToken, email);
    }

    private AuthUser restoreAndUpdate(AuthUser user, Jwt idToken, String email) {
        if (user.isDeleted()) {
            user.setDeleted(false);
            user.setEnabled(true);
            user.setActiveStatus(true);
        }
        return updateProfile(user, idToken, email);
    }

    private AuthUser updateProfile(AuthUser user, Jwt idToken, String email) {
        user.setEmail(email);
        user.setFirstName(idToken.getClaimAsString("given_name"));
        user.setLastName(idToken.getClaimAsString("family_name"));
        return user;
    }

    private String requiredClaim(Jwt idToken, String claimName) {
        String value = idToken.getClaimAsString(claimName);
        if (value == null || value.isBlank()) {
            throw new BadCredentialsException("Google token is missing " + claimName);
        }
        return value;
    }
}
