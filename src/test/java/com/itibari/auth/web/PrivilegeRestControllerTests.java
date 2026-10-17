package com.itibari.auth.web;

import com.itibari.auth.AuthIssuerApplicationTests;
import com.itibari.auth.model.Privilege;
import com.itibari.auth.model.request.PrivilegeDto;
import com.itibari.auth.model.request.PrivilegeGroupDto;
import com.itibari.auth.model.request.PrivilegeGroupPrivilegeDto;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class PrivilegeRestControllerTests extends AuthIssuerApplicationTests {

    @Test
    void createPrivilegeWorks() {
        String email = uniqueEmail("priv-admin");
        saveUser(email, "password123");
        String token = login(email, "password123");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(PrivilegeDto.builder()
                        .name("VIEW_USERS")
                        .build())
                .post("/privileges")
                .then().log().all()
                .statusCode(200)
                .body("message", equalTo("success"))
                .body("identity", equalTo("VIEW_USERS"));
    }

    @Test
    void listPrivilegesReturnsCreatedPrivileges() {
        String email = uniqueEmail("priv-list");
        saveUser(email, "password123");
        String token = login(email, "password123");

        Privilege p1 = savePrivilege("VIEW_USERS");
        Privilege p2 = savePrivilege("VIEW_SALES");

        given()
                .auth().oauth2(token)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .get("/privileges")
                .then().log().all()
                .statusCode(200)
                .body("content.name", hasItems("VIEW_USERS", "VIEW_SALES"))
                .body("content.publicId", hasItems(p1.getPublicId().toString(), p2.getPublicId().toString()));
    }

    @Test
    void createPrivilegeGroupAndAssignPrivilegesWorks() {
        String email = uniqueEmail("priv-group");
        saveUser(email, "password123");
        String token = login(email, "password123");
        Privilege privilege = savePrivilege("EDIT_PRODUCT");
        UUID tenantId = UUID.randomUUID();

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(new PrivilegeGroupDto("Inventory", tenantId))
                .post("/privilege-groups")
                .then().log().all()
                .statusCode(200)
                .body("message", equalTo("successfully created privilege group"))
                .body("identity", equalTo("Inventory"));

        String publicId = given()
                .auth().oauth2(token)
                .queryParam("parentTenantPublicId", tenantId)
                .get("/privilege-groups")
                .then().log().all()
                .statusCode(200)
                .body("content.name", hasItem("Inventory"))
                .extract()
                .path("content[0].publicId");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(new PrivilegeGroupPrivilegeDto(List.of(privilege.getPublicId())))
                .patch("/privilege-groups/{groupPublicId}/privileges", publicId)
                .then().log().all()
                .statusCode(200)
                .body("message", equalTo("successfully added privileges"))
                .body("identity", containsString("EDIT_PRODUCT"));
    }
}
