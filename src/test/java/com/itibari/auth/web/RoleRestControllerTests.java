package com.itibari.auth.web;

import com.itibari.auth.AuthIssuerApplicationTests;
import com.itibari.auth.model.Privilege;
import com.itibari.auth.model.Role;
import com.itibari.auth.model.request.CreateRoleDTO;
import com.itibari.auth.model.request.RolePrivilegeDto;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class RoleRestControllerTests extends AuthIssuerApplicationTests {

    @Test
    void createRoleWorks() {
        String email = uniqueEmail("role-admin");
        saveUser(email, "password123");
        String token = login(email, "password123");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(CreateRoleDTO.builder()
                        .name("MANAGER")
                        .description("Store manager")
                        .parentTenantPublicId(UUID.randomUUID())
                        .build())
                .post("/roles")
                .then().log().all()
                .statusCode(200)
                .body("name", equalTo("MANAGER"))
                .body("publicId", not(blankOrNullString()));
    }

    @Test
    void listRolesReturnsCreatedRoles() {
        String email = uniqueEmail("role-list");
        saveUser(email, "password123");
        String token = login(email, "password123");

        Role roleA = saveRole("ROLE_A");
        Role roleB = saveRole("ROLE_B");

        given()
                .auth().oauth2(token)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .get("/roles")
                .then().log().all()
                .statusCode(200)
                .body("content.name", hasItems("ROLE_A", "ROLE_B"))
                .body("content.publicId", hasItems(roleA.getPublicId().toString(), roleB.getPublicId().toString()));
    }

    @Test
    void assignPrivilegeToRoleWorks() {
        String email = uniqueEmail("role-priv");
        saveUser(email, "password123");
        String token = login(email, "password123");

        Role role = saveRole("STOCK_CLERK");
        Privilege privilege = savePrivilege("VIEW_STOCK");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(RolePrivilegeDto.builder()
                        .privilegeUUIDs(List.of(privilege.getPublicId()))
                        .build())
                .patch("/roles/assignPrivilegeToRole/{rolePublicId}", role.getPublicId())
                .then().log().all()
                .statusCode(200)
                .body("message", equalTo("successfully added privilege"))
                .body("identity", containsString("VIEW_STOCK"));
    }

    @Test
    void listTenantRolesReturnsOnlyParentTenantRoles() {
        String email = uniqueEmail("role-tenant");
        saveUser(email, "password123");
        String token = login(email, "password123");
        UUID tenantId = UUID.fromString(createParentShop(token, "Tenant Shop"));

        Role tenantRole = saveRole("CASHIER", tenantId);
        saveRole("OTHER_TENANT_ROLE", UUID.randomUUID());
        saveRole("GLOBAL_ROLE");

        given()
                .auth().oauth2(token)
                .queryParam("searchParam", "CASHIER")
                .get("/roles/tenant")
                .then().log().all()
                .statusCode(200)
                .body("totalElements", equalTo(1))
                .body("content[0].name", equalTo("CASHIER"))
                .body("content[0].publicId", equalTo(tenantRole.getPublicId().toString()));
    }

    @Test
    void createRoleWithoutTenantIdIsListedForLoggedInShop() {
        String email = uniqueEmail("role-ui-create");
        saveUser(email, "password123");
        String token = login(email, "password123");
        createParentShop(token, "Tenant Shop");

        String publicId = given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(CreateRoleDTO.builder()
                        .name("UI_CASHIER")
                        .description("Created like the UI, without parentTenantPublicId")
                        .build())
                .post("/roles")
                .then().log().all()
                .statusCode(200)
                .body("name", equalTo("UI_CASHIER"))
                .extract()
                .path("publicId");

        given()
                .auth().oauth2(token)
                .get("/roles/tenant")
                .then().log().all()
                .statusCode(200)
                .body("content.name", hasItem("UI_CASHIER"))
                .body("content.publicId", hasItem(publicId));
    }

    @Test
    void listTenantRolesRequiresParentShop() {
        String email = uniqueEmail("role-no-shop");
        saveUser(email, "password123");
        String token = login(email, "password123");

        given()
                .auth().oauth2(token)
                .get("/roles/tenant")
                .then().log().all()
                .statusCode(404);
    }
}
