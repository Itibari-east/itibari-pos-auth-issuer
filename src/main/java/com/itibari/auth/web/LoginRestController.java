package com.itibari.auth.web;

import com.itibari.auth.exception.WrongPasswordException;
import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.LoginRequest;
import com.itibari.auth.model.TokenResponse;
import com.itibari.auth.model.request.GoogleLoginRequest;
import com.itibari.auth.service.AuthUserLookupService;
import com.itibari.auth.service.GoogleLoginService;
import com.itibari.auth.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/login")
public class LoginRestController {

    private final AuthUserLookupService authUserLookupService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final GoogleLoginService googleLoginService;
    private final JwtDecoder googleIdTokenDecoder;

    public LoginRestController(AuthUserLookupService authUserLookupService,
                               PasswordEncoder passwordEncoder,
                               TokenService tokenService,
                               GoogleLoginService googleLoginService,
                               @Qualifier("googleIdTokenDecoder") JwtDecoder googleIdTokenDecoder) {
        this.authUserLookupService = authUserLookupService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.googleLoginService = googleLoginService;
        this.googleIdTokenDecoder = googleIdTokenDecoder;
    }

    @PostMapping
    @Operation(summary = "Password login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthUser user = authUserLookupService.findByIdentifier(request.identifier())
                .orElseThrow(WrongPasswordException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new WrongPasswordException();
        }

        return ResponseEntity.ok(tokenService.generateToken(user));
    }

    @PostMapping("/google")
    @Operation(summary = "Google login")
    public ResponseEntity<TokenResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        AuthUser user = googleLoginService.findOrCreateUser(googleIdTokenDecoder.decode(request.idToken()));
        return ResponseEntity.ok(tokenService.generateGoogleToken(user));
    }
}
