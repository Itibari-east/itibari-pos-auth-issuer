package com.itibari.auth.web;

import com.itibari.auth.AuthIssuerApplicationTests;
import com.itibari.auth.model.LoginRequest;
import com.itibari.auth.model.request.GoogleLoginRequest;
import com.itibari.auth.repository.AuthUserRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class LoginRestControllerTests extends AuthIssuerApplicationTests {

    @Autowired
    private AuthUserRepository authUserRepository;

    @MockitoBean(name = "googleIdTokenDecoder")
    private JwtDecoder googleIdTokenDecoder;

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
}
