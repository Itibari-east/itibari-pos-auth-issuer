package com.itibari.auth.web;

import com.itibari.auth.AuthIssuerApplicationTests;
import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.LoginRequest;
import com.itibari.auth.model.request.ChangePasswordRequest;
import com.itibari.auth.model.request.GoogleLoginRequest;
import com.itibari.auth.model.request.ResetPasswordRequest;
import com.itibari.auth.repository.AuthUserRepository;
import com.itibari.auth.service.NotificationClient;
import com.itibari.auth.service.ResetTokenService;
import com.itibari.auth.util.date.FormatDate;
import com.itibari.auth.validation.StrongPassword;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class LoginRestControllerTests extends AuthIssuerApplicationTests {

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private ResetTokenService resetTokenService;

    @MockitoBean(name = "googleIdTokenDecoder")
    private JwtDecoder googleIdTokenDecoder;

    @MockitoBean
    private NotificationClient notificationClient;

    @Test
    void loginShouldReturnAccessToken() {
        String email = uniqueEmail("login");
        saveUser(email, "password123");

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(email, "password123"))
                .post("/login")
                .then().log().all()
                .statusCode(200)
                .body("accessToken", not(blankOrNullString()))
                .body("subscription", equalTo(false));
    }

    @Test
    void loginShouldRejectInvalidCredentials() {
        String email = uniqueEmail("bad-login");
        saveUser(email, "password123");

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(email, "wrong-password"))
                .post("/login")
                .then().log().all()
                .statusCode(401)
                .body("detail", equalTo("Invalid credentials"));
    }

    @Test
    void googleLoginShouldCreateUserAndReturnToken() {
        String subject = "google-sub-" + System.nanoTime();
        String email = uniqueEmail("google");

        when(googleIdTokenDecoder.decode(anyString())).thenReturn(Jwt.withTokenValue("google-id-token")
                .header("alg", "RS256")
                .subject(subject)
                .claims(claims -> {
                    claims.put("email", email);
                    claims.put("email_verified", true);
                    claims.put("given_name", "Google");
                    claims.put("family_name", "User");
                    claims.put("aud", List.of("test-google-client"));
                })
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build());

        given()
                .contentType(ContentType.JSON)
                .body(new GoogleLoginRequest("fake-google-id-token"))
                .post("/login/google")
                .then().log().all()
                .statusCode(200)
                .body("accessToken", not(blankOrNullString()));

        given()
                .contentType(ContentType.JSON)
                .body(new GoogleLoginRequest("fake-google-id-token"))
                .post("/login/google")
                .then()
                .statusCode(200);

        org.junit.jupiter.api.Assertions.assertTrue(
                authUserRepository.findByGoogleSubjectAndDeletedFalse(subject).isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(1,
                authUserRepository.findByEmailIgnoreCaseAndDeletedFalse(email).stream().count());
    }

    @Test
    void authenticatedEndpointRejectsMissingToken() {
        given()
                .get("/users/me")
                .then().log().all()
                .statusCode(401);
    }

    @Test
    void loginShouldRejectUsersWhoMustResetPassword() {
        String email = uniqueEmail("force-reset");
        AuthUser user = saveUser(email, "password123");
        user.setForcePasswordReset(true);
        authUserRepository.save(user);

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(email, "password123"))
                .post("/login")
                .then().log().all()
                .statusCode(403)
                .body("detail", equalTo("Password reset is required before login"));
    }

    @Test
    void forgetPasswordShouldIssueResetTokenAndForceReset() {
        String email = uniqueEmail("forgot");
        saveUser(email, "password123");

        given()
                .post("/login/forgetPassword?identifier=" + email)
                .then().log().all()
                .statusCode(200)
                .body("resetToken", matchesRegex("\\d{6}"))
                .body("expiresAt", equalTo("5"));

        AuthUser savedUser = authUserRepository.findByEmailIgnoreCaseAndDeletedFalse(email).orElseThrow();
        assertTrue(savedUser.isForcePasswordReset());
        assertNotNull(savedUser.getResetPasswordTokenHash());
        assertNotNull(savedUser.getResetPasswordTokenLookupHash());
        assertNotNull(savedUser.getResetPasswordTokenExpiresAt());
    }

    @Test
    void forgetPasswordShouldRejectUnknownUser() {
        given()
                .post("/login/forgetPassword?identifier=missing@example.com")
                .then().log().all()
                .statusCode(404)
                .body("detail", equalTo("User not found"));
    }

    @Test
    void resetPasswordShouldClearForceFlagAndAllowLogin() {
        String email = uniqueEmail("reset-ok");
        saveUser(email, "password123");
        String resetToken = requestResetToken(email);

        given()
                .contentType(ContentType.JSON)
                .body(new ResetPasswordRequest(resetToken, "Password1!", "Password1!"))
                .post("/login/resetPassword")
                .then().log().all()
                .statusCode(200)
                .body("message", equalTo("You have successfully reset your password"));

        AuthUser savedUser = authUserRepository.findByEmailIgnoreCaseAndDeletedFalse(email).orElseThrow();
        assertFalse(savedUser.isForcePasswordReset());

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(email, "password123"))
                .post("/login")
                .then()
                .statusCode(401);

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(email, "Password1!"))
                .post("/login")
                .then()
                .statusCode(200)
                .body("accessToken", not(blankOrNullString()));
    }

    @Test
    void resetPasswordShouldAcceptSetupLinkToken() {
        String email = uniqueEmail("setup-link");
        AuthUser user = saveUser(email, "temp-password");
        String setupToken = resetTokenService.generateUniqueLinkToken();
        user.setForcePasswordReset(true);
        user.setResetPasswordTokenHash(passwordEncoder.encode(setupToken));
        user.setResetPasswordTokenLookupHash(resetTokenService.lookupHash(setupToken));
        user.setResetPasswordTokenExpiresAt(FormatDate.currentNairobiDateTime().plusHours(48));
        authUserRepository.save(user);

        given()
                .contentType(ContentType.JSON)
                .body(new ResetPasswordRequest(setupToken, "Password1!", "Password1!"))
                .post("/login/resetPassword")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(email, "Password1!"))
                .post("/login")
                .then()
                .statusCode(200)
                .body("accessToken", not(blankOrNullString()));
    }

    @Test
    void resetPasswordShouldRejectMismatchedConfirmation() {
        String email = uniqueEmail("reset-mismatch");
        saveUser(email, "password123");
        String resetToken = requestResetToken(email);

        given()
                .contentType(ContentType.JSON)
                .body(new ResetPasswordRequest(resetToken, "Password1!", "different"))
                .post("/login/resetPassword")
                .then()
                .statusCode(400)
                .body("detail", equalTo("Password do not match"));

        assertTrue(authUserRepository.findByEmailIgnoreCaseAndDeletedFalse(email).orElseThrow().isForcePasswordReset());
    }

    @Test
    void resetPasswordShouldRejectInvalidResetToken() {
        String email = uniqueEmail("reset-invalid");
        saveUser(email, "password123");
        requestResetToken(email);

        given()
                .contentType(ContentType.JSON)
                .body(new ResetPasswordRequest("111111", "Password1!", "Password1!"))
                .post("/login/resetPassword")
                .then()
                .statusCode(400)
                .body("detail", equalTo("Invalid or expired reset token"));
    }

    @Test
    void resetPasswordShouldRejectExpiredResetToken() {
        String email = uniqueEmail("reset-expired");
        saveUser(email, "password123");
        String resetToken = requestResetToken(email);

        AuthUser savedUser = authUserRepository.findByEmailIgnoreCaseAndDeletedFalse(email).orElseThrow();
        savedUser.setResetPasswordTokenExpiresAt(FormatDate.currentNairobiDateTime().minusMinutes(1));
        authUserRepository.save(savedUser);

        given()
                .contentType(ContentType.JSON)
                .body(new ResetPasswordRequest(resetToken, "Password1!", "Password1!"))
                .post("/login/resetPassword")
                .then()
                .statusCode(400)
                .body("detail", equalTo("Invalid or expired reset token"));
    }

    @Test
    void resetPasswordShouldRejectUsersWhoDoNotNeedReset() {
        String email = uniqueEmail("reset-not-required");
        saveUser(email, "password123");

        given()
                .contentType(ContentType.JSON)
                .body(new ResetPasswordRequest("123456", "Password1!", "Password1!"))
                .post("/login/resetPassword")
                .then()
                .statusCode(400)
                .body("detail", equalTo("Invalid or expired reset token"));
    }

    @Test
    void changePasswordShouldUpdatePasswordForAuthenticatedUser() {
        String email = uniqueEmail("change-ok");
        saveUser(email, "password123");
        String token = login(email, "password123");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(new ChangePasswordRequest("Password1!", "Password1!"))
                .post("/login/changePassword")
                .then().log().all()
                .statusCode(200)
                .body("message", equalTo("You have successfully changed your password"));

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(email, "password123"))
                .post("/login")
                .then()
                .statusCode(401);

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(email, "Password1!"))
                .post("/login")
                .then()
                .statusCode(200)
                .body("accessToken", not(blankOrNullString()));
    }

    @Test
    void changePasswordShouldRejectMissingToken() {
        given()
                .contentType(ContentType.JSON)
                .body(new ChangePasswordRequest("Password1!", "Password1!"))
                .post("/login/changePassword")
                .then().log().all()
                .statusCode(401);
    }

    @Test
    void changePasswordShouldRejectMismatchedConfirmation() {
        String email = uniqueEmail("change-mismatch");
        saveUser(email, "password123");
        String token = login(email, "password123");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(new ChangePasswordRequest("Password1!", "different"))
                .post("/login/changePassword")
                .then()
                .statusCode(400)
                .body("detail", equalTo("Password do not match"));

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(email, "password123"))
                .post("/login")
                .then()
                .statusCode(200);
    }

    @Test
    void changePasswordShouldRejectCurrentPassword() {
        String email = uniqueEmail("change-same");
        saveUser(email, "Password1!");
        String token = login(email, "Password1!");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(new ChangePasswordRequest("Password1!", "Password1!"))
                .post("/login/changePassword")
                .then()
                .statusCode(400)
                .body("detail", equalTo("New password must be different from the current password"));
    }

    @Test
    void resetPasswordShouldRejectWeakPassword() {
        String email = uniqueEmail("reset-weak");
        saveUser(email, "password123");
        String resetToken = requestResetToken(email);

        given()
                .contentType(ContentType.JSON)
                .body(new ResetPasswordRequest(resetToken, "weakpass", "weakpass"))
                .post("/login/resetPassword")
                .then()
                .statusCode(400)
                .body("detail", containsString(StrongPassword.MESSAGE));
    }

    @Test
    void changePasswordShouldRejectWeakPassword() {
        String email = uniqueEmail("change-weak");
        saveUser(email, "password123");
        String token = login(email, "password123");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(new ChangePasswordRequest("weakpass", "weakpass"))
                .post("/login/changePassword")
                .then()
                .statusCode(400)
                .body("detail", containsString(StrongPassword.MESSAGE));
    }

    private String requestResetToken(String email) {
        return given()
                .post("/login/forgetPassword?identifier=" + email)
                .then()
                .statusCode(200)
                .extract()
                .path("resetToken");
    }
}
