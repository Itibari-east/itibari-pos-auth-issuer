package com.itibari.auth.web;

import com.itibari.auth.AuthIssuerApplicationTests;
import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.Institution;
import com.itibari.auth.model.LoginRequest;
import com.itibari.auth.model.Role;
import com.itibari.auth.model.request.AuthUserRequest;
import com.itibari.auth.model.request.CreateUserDTO;
import com.itibari.auth.model.request.UserRoleDto;
import com.itibari.auth.repository.InstitutionRepository;
import com.itibari.auth.validation.StrongPassword;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class UserRestControllerTests extends AuthIssuerApplicationTests {

    @Autowired
    private InstitutionRepository institutionRepository;

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
                        .password("Password1!")
                        .confirmPassword("Password1!")
                        .build())
                .post("/users")
                .then().log().all()
                .statusCode(200)
                .body("message", equalToIgnoringCase("User created successfully"))
                .body("identity", equalToIgnoringCase("Ada Lovelace"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Short1!", "noupper1!", "NOLOWER1!", "NoNumber!", "NoSpecial1"})
    void createUserShouldRejectWeakPassword(String password) {
        String email = uniqueEmail("weak");

        given()
                .contentType(ContentType.JSON)
                .body(CreateUserDTO.builder()
                        .firstName("Ada")
                        .lastName("Lovelace")
                        .contactEmail(email)
                        .contactPhoneNumber("+255700000003")
                        .password(password)
                        .confirmPassword(password)
                        .build())
                .post("/users")
                .then()
                .statusCode(400)
                .body("detail", containsString(StrongPassword.MESSAGE));
    }

    @Test
    void createdUserCanLogIn() {
        String email = uniqueEmail("created-login");
        String password = "Password1!";

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
                .body("activeStatus", equalTo(true))
                .body("passportUrl", nullValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/users/me", "/users/currentUser"})
    void currentUserReturnsPassportUrl(String path) {
        String email = uniqueEmail("passport");
        AuthUser user = saveUser(email, "password123");
        String token = login(email, "password123");
        String passportUrl = "https://api-pos-dev.itibari.io/api/v1/pos/files/"
                + UUID.randomUUID() + "/content";
        when(fileFeignClient.latestFileUrls(eq("PASSPORT"), anyList()))
                .thenReturn(Map.of(user.getPublicId(), passportUrl));

        given()
                .auth().oauth2(token)
                .get(path)
                .then().log().all()
                .statusCode(200)
                .body("email", equalToIgnoringCase(email))
                .body("passportUrl", equalTo(passportUrl));
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

    @Test
    void downloadUserAccessReportPdfWorks() throws Exception {
        String email = uniqueEmail("access-pdf");
        AuthUser user = saveUser(email, "password123");
        String token = login(email, "password123");
        createParentShop(token, "PDF Shop");

        byte[] pdf = given()
                .auth().oauth2(token)
                .get("/users/{userId}/access-report", user.getPublicId())
                .then().log().all()
                .statusCode(200)
                .contentType("application/pdf")
                .header("Content-Disposition", containsString("access-report.pdf"))
                .extract()
                .asByteArray();

        assertTrue(pdf.length > 100);
        assertTrue(new String(pdf, 0, 4).startsWith("%PDF"));

        PdfReader reader = new PdfReader(pdf);
        try {
            String text = new PdfTextExtractor(reader, true).getTextFromPage(1, true)
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("\\s+", " ");
            assertTrue(text.contains("User Access Report"), text);
            assertTrue(text.replace(" ", "").toLowerCase().contains(email.replace(" ", "").toLowerCase()), text);
            assertTrue(text.contains("Owner"), text);
            assertTrue(text.contains("SALES"), text);
            assertTrue(text.contains("VIEW_SALES"), text);
            assertTrue(text.contains("PDF Shop"), text);
        } finally {
            reader.close();
        }
    }

    @Test
    void parentShopCallerCanCreateChildUser() {
        String email = uniqueEmail("parent-caller");
        Institution parent = saveParent("Parent Shop");
        Institution branch = saveBranch("Child Branch", parent);
        Role role = saveRole("Cashier", parent.getPublicId());
        callerOn(email, parent);
        String token = login(email, "password123");

        postChildUser(token, role.getPublicId(), branch.getPublicId(), uniqueEmail("child"))
                .statusCode(201)
                .body("message", equalTo("User created successfully"))
                .body("identity", equalTo("Ada Lovelace"))
                .body("publicId", notNullValue());
    }

    @Test
    void branchCallerCanCreateChildUser() {
        String email = uniqueEmail("branch-caller");
        Institution parent = saveParent("Parent Shop");
        Institution branch = saveBranch("Child Branch", parent);
        Role role = saveRole("Cashier", parent.getPublicId());
        callerOn(email, branch);
        String token = login(email, "password123");

        postChildUser(token, role.getPublicId(), branch.getPublicId(), uniqueEmail("child"))
                .statusCode(201)
                .body("message", equalTo("User created successfully"))
                .body("identity", equalTo("Ada Lovelace"))
                .body("publicId", notNullValue());
    }

    @Test
    void branchCallerRejectsRoleFromAnotherCompany() {
        String email = uniqueEmail("foreign-role");
        Institution parent = saveParent("Parent Shop");
        Institution branch = saveBranch("Child Branch", parent);
        Institution other = saveParent("Other Shop");
        Role foreignRole = saveRole("Foreign", other.getPublicId());
        callerOn(email, branch);
        String token = login(email, "password123");

        postChildUser(token, foreignRole.getPublicId(), branch.getPublicId(), uniqueEmail("child"))
                .statusCode(404)
                .body("detail", equalTo("Role not found"));
    }

    @Test
    void branchCallerRejectsShopUnderAnotherParent() {
        String email = uniqueEmail("foreign-shop");
        Institution parent = saveParent("Parent Shop");
        Institution branch = saveBranch("Child Branch", parent);
        Institution other = saveParent("Other Shop");
        Institution otherBranch = saveBranch("Other Branch", other);
        Role role = saveRole("Cashier", parent.getPublicId());
        callerOn(email, branch);
        String token = login(email, "password123");

        postChildUser(token, role.getPublicId(), otherBranch.getPublicId(), uniqueEmail("child"))
                .statusCode(404)
                .body("detail", equalTo("Shop not found"));
    }

    @Test
    void callerWithoutShopCannotCreateChildUser() {
        String email = uniqueEmail("no-shop");
        saveUser(email, "password123");
        String token = login(email, "password123");

        postChildUser(token, UUID.randomUUID(), UUID.randomUUID(), uniqueEmail("child"))
                .statusCode(404)
                .body("detail", equalTo("Parent shop not found"));
    }

    @Test
    void callerOnTwoCompaniesCannotCreateChildUser() {
        String email = uniqueEmail("two-parents");
        Institution firstBranch = saveBranch("First Branch", saveParent("First Shop"));
        Institution secondBranch = saveBranch("Second Branch", saveParent("Second Shop"));
        callerOn(email, firstBranch, secondBranch);
        String token = login(email, "password123");

        postChildUser(token, UUID.randomUUID(), UUID.randomUUID(), uniqueEmail("child"))
                .statusCode(404)
                .body("detail", equalTo("Parent shop not found"));
    }

    @Test
    void branchCallerStillCannotListUsersOrRoles() {
        String email = uniqueEmail("branch-list");
        Institution branch = saveBranch("Child Branch", saveParent("Parent Shop"));
        callerOn(email, branch);
        String token = login(email, "password123");

        given()
                .auth().oauth2(token)
                .get("/users/childUser")
                .then()
                .statusCode(404)
                .body("detail", equalTo("Parent shop not found"));
        given()
                .auth().oauth2(token)
                .get("/roles")
                .then()
                .statusCode(404)
                .body("detail", equalTo("Parent shop not found"));
    }

    @Test
    void childUserStoresKenyanPhoneUnchanged() {
        String email = uniqueEmail("parent-ke");
        Institution parent = saveParent("Parent Shop");
        Institution branch = saveBranch("Child Branch", parent);
        Role role = saveRole("Cashier", parent.getPublicId());
        callerOn(email, parent);
        String token = login(email, "password123");
        String childEmail = uniqueEmail("child-ke");
        String phone = "+254712345678";

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(childUserBody(role.getPublicId(), branch.getPublicId(), childEmail, phone))
                .post("/users/childUser")
                .then()
                .statusCode(201);

        AuthUser created = authUserRepository.findByEmailIgnoreCaseAndDeletedFalse(childEmail).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(phone, created.getPhoneNumber());
    }

    @Test
    void childUserStoresTanzanianPhoneUnchanged() {
        String email = uniqueEmail("parent-tz");
        Institution parent = saveParent("Parent Shop");
        Institution branch = saveBranch("Child Branch", parent);
        Role role = saveRole("Cashier", parent.getPublicId());
        callerOn(email, parent);
        String token = login(email, "password123");
        String childEmail = uniqueEmail("child-tz");
        String phone = "+255712345678";

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(childUserBody(role.getPublicId(), branch.getPublicId(), childEmail, phone))
                .post("/users/childUser")
                .then()
                .statusCode(201);

        AuthUser created = authUserRepository.findByEmailIgnoreCaseAndDeletedFalse(childEmail).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(phone, created.getPhoneNumber());
    }

    @Test
    void childUserRejectsImpossiblePhoneLength() {
        String email = uniqueEmail("parent-bad-phone");
        Institution parent = saveParent("Parent Shop");
        Institution branch = saveBranch("Child Branch", parent);
        Role role = saveRole("Cashier", parent.getPublicId());
        callerOn(email, parent);
        String token = login(email, "password123");
        String childEmail = uniqueEmail("child-bad");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(childUserBody(role.getPublicId(), branch.getPublicId(), childEmail, "+2541"))
                .post("/users/childUser")
                .then()
                .statusCode(400)
                .body("detail", equalTo("Invalid phone number"));

        org.junit.jupiter.api.Assertions.assertTrue(
                authUserRepository.findByEmailIgnoreCaseAndDeletedFalse(childEmail).isEmpty());
    }

    @Test
    void createChildUserRequiresBearerToken() {
        given()
                .contentType(ContentType.JSON)
                .body(childUserBody(UUID.randomUUID(), UUID.randomUUID(), uniqueEmail("anon")))
                .post("/users/childUser")
                .then()
                .statusCode(401);
    }

    private Institution saveParent(String name) {
        return institutionRepository.save(Institution.builder()
                .name(name)
                .identityNumber(UUID.randomUUID().toString())
                .parentTenant(true)
                .build());
    }

    private Institution saveBranch(String name, Institution parent) {
        return institutionRepository.save(Institution.builder()
                .name(name)
                .identityNumber(UUID.randomUUID().toString())
                .parent(parent)
                .build());
    }

    private void callerOn(String email, Institution... shops) {
        AuthUser caller = saveUser(email, "password123");
        caller.getInstitutions().addAll(List.of(shops));
        authUserRepository.save(caller);
    }

    private ValidatableResponse postChildUser(String token, UUID roleId, UUID branchId, String email) {
        return given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(childUserBody(roleId, branchId, email))
                .post("/users/childUser")
                .then();
    }

    private Map<String, Object> childUserBody(UUID roleId, UUID branchId, String email) {
        return childUserBody(roleId, branchId, email, nextE164Phone());
    }

    private Map<String, Object> childUserBody(UUID roleId, UUID branchId, String email, String phone) {
        return Map.of(
                "firstName", "Ada",
                "lastName", "Lovelace",
                "role", List.of(roleId),
                "branch", List.of(branchId),
                "phone", phone,
                "email", email);
    }
}
