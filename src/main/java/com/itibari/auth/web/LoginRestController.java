package com.itibari.auth.web;

import com.itibari.auth.doc.Examples;
import com.itibari.auth.exception.WrongPasswordException;
import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.LoginRequest;
import com.itibari.auth.model.TokenResponse;
import com.itibari.auth.model.request.GoogleLoginRequest;
import com.itibari.auth.model.request.ResetPasswordRequest;
import com.itibari.auth.model.response.MessageResponse;
import com.itibari.auth.model.response.ResetPasswordTokenResponse;
import com.itibari.auth.service.AuthUserLookupService;
import com.itibari.auth.service.GoogleLoginService;
import com.itibari.auth.service.ResetService;
import com.itibari.auth.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final ResetService resetService;

    public LoginRestController(AuthUserLookupService authUserLookupService,
                               PasswordEncoder passwordEncoder,
                               TokenService tokenService,
                               GoogleLoginService googleLoginService,
                               @Qualifier("googleIdTokenDecoder") JwtDecoder googleIdTokenDecoder,
                               ResetService resetService) {
        this.authUserLookupService = authUserLookupService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.googleLoginService = googleLoginService;
        this.googleIdTokenDecoder = googleIdTokenDecoder;
        this.resetService = resetService;
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

    @PostMapping("/forgetPassword")
    @Operation(summary = "Request password reset",
            parameters = @Parameter(name = "identifier", required = true, example = "alice@example.com"),
            responses = @ApiResponse(responseCode = "200", description = "Password reset token created", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = Examples.FORGOT_PASSWORD_RESPONSE))))
    public ResponseEntity<ResetPasswordTokenResponse> forgetPassword(
            @RequestParam(name = "identifier") String identifier) {
        return ResponseEntity.ok(resetService.requestPasswordChange(identifier));
    }

    @PostMapping("/resetPassword")
    @Operation(summary = "Reset password",
            responses = @ApiResponse(responseCode = "200", description = "Password reset", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = Examples.RESET_PASSWORD_RESPONSE))),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
                    examples = @ExampleObject(name = "Reset password request", value = Examples.RESET_PASSWORD_REQUEST))))
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("You have successfully reset your password"));
    }
}
