package com.itibari.auth.web;

import com.itibari.auth.AuthIssuerApplicationTests;
import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.LoginRequest;
import com.itibari.auth.model.Role;
import com.itibari.auth.model.request.AuthUserRequest;
import com.itibari.auth.model.request.CreateUserDTO;
import com.itibari.auth.model.request.UserRoleDto;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class UserRestControllerTests extends AuthIssuerApplicationTests {

    @Test
    void createUserWorks() {
        String email = uniqueEmail("create");

        given()
                .contentType(ContentType.JSON)
                .body(CreateUserDTO.builder()
                        .firstName("Ada")
                        .lastName("Lovelace")
                        .contactEmail(email)
                        .contactPhoneNumber("+255700000001")
                        .password("secure-password")
                        .confirmPassword("secure-password")
                        .build())
                .post("/users")
                .then().log().all()
                .statusCode(200)
                .body("message", equalToIgnoringCase("User created successfully"))
                .body("identity", equalToIgnoringCase("Ada Lovelace"));
    }

    @Test
    void createdUserCanLogIn() {
        String email = uniqueEmail("created-login");
        String password = "secure-password";

        given()
                .contentType(ContentType.JSON)
                .body(CreateUserDTO.builder()
                        .firstName("Login")
                        .lastName("User")
                        .contactEmail(email)
                        .contactPhoneNumber("+255700000002")
                        .password(password)
                        .confirmPassword(password)
                        .build())
                .post("/users")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(email, password))
                .post("/login")
                .then().log().all()
                .statusCode(200)
                .body("accessToken", not(blankOrNullString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/users/me", "/users/currentUser"})
    void currentUserRequiresAuthAndReturnsProfile(String path) {
        String email = uniqueEmail("me");
        saveUser(email, "password123");
        String token = login(email, "password123");

        given()
                .auth().oauth2(token)
                .get(path)
                .then().log().all()
                .statusCode(200)
                .body("email", equalToIgnoringCase(email))
                .body("firstName", equalTo("Test"))
                .body("lastName", equalTo("User"))
                .body("activeStatus", equalTo(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/users/me", "/users/currentUser"})
    void currentUserRejectsUnauthenticatedRequests(String path) {
        given().get(path).then().statusCode(401);
    }

    @Test
    void listUsersReturnsCreatedUser() {
        String email = uniqueEmail("list");
        saveUser(email, "password123");
        String token = login(email, "password123");

        given()
                .auth().oauth2(token)
                .queryParam("searchParam", email)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .get("/users")
                .then().log().all()
                .statusCode(200)
                .body("totalElements", greaterThanOrEqualTo(1))
                .body("content.email", hasItem(equalToIgnoringCase(email)));
    }

    @Test
    void updateUserWorks() {
        String email = uniqueEmail("update");
        AuthUser user = saveUser(email, "password123");
        String token = login(email, "password123");
        String newEmail = uniqueEmail("updated");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(new AuthUserRequest("Updated", "Name", "+255711111111", newEmail))
                .patch("/users/{publicId}", user.getPublicId())
                .then().log().all()
                .statusCode(200)
                .body("firstName", equalTo("Updated"))
                .body("lastName", equalTo("Name"))
                .body("email", equalToIgnoringCase(newEmail));
    }

    @Test
    void assignRolesToUserWorks() {
        String email = uniqueEmail("roles");
        AuthUser user = saveUser(email, "password123");
        Role role = saveRole("CASHIER");
        String token = login(email, "password123");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(new UserRoleDto(List.of(role.getPublicId())))
                .patch("/users/{userId}/roles", user.getPublicId())
                .then().log().all()
                .statusCode(200)
                .body("message", equalTo("successfully assigned roles"))
                .body("identity", containsString("CASHIER"));

        given()
                .auth().oauth2(token)
                .get("/users/{userId}/roles", user.getPublicId())
                .then().log().all()
                .statusCode(200)
                .body("content.name", hasItem("CASHIER"))
                .body("content.publicId", hasItem(role.getPublicId().toString()));
    }

    @Test
    void getUnknownUserReturns404() {
        String email = uniqueEmail("missing");
        saveUser(email, "password123");
        String token = login(email, "password123");

        given()
                .auth().oauth2(token)
                .get("/users/{publicId}", UUID.randomUUID())
                .then().log().all()
                .statusCode(404);
    }
}
