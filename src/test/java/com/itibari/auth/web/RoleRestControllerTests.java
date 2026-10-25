package com.itibari.auth.web;

import com.itibari.auth.AuthIssuerApplicationTests;
import com.itibari.auth.access.AccessCatalog;
import com.itibari.auth.model.Privilege;
import com.itibari.auth.model.Role;
import com.itibari.auth.model.request.CreateRoleDTO;
import com.itibari.auth.model.request.RolePrivilegeDto;
import com.itibari.auth.model.request.RolePrivilegeGroupDto;
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
    void listRolesReturnsParentTenantRoles() {
        String email = uniqueEmail("role-list");
        saveUser(email, "password123");
        String token = login(email, "password123");
        UUID tenantId = UUID.fromString(createParentShop(token, "Tenant Shop"));

        Role roleA = saveRole("ROLE_A", tenantId);
        Role roleB = saveRole("ROLE_B", tenantId);
        saveRole("OTHER_TENANT_ROLE", UUID.randomUUID());
        saveRole("GLOBAL_ROLE");

        given()
                .auth().oauth2(token)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .get("/roles")
                .then().log().all()
                .statusCode(200)
                .body("content.name", hasItems("ROLE_A", "ROLE_B"))
                .body("content.name", not(hasItems("OTHER_TENANT_ROLE", "GLOBAL_ROLE")))
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

        Role tenantRole = saveRole("NIGHT_CASHIER", tenantId);
        saveRole("OTHER_TENANT_ROLE", UUID.randomUUID());
        saveRole("GLOBAL_ROLE");

        given()
                .auth().oauth2(token)
                .queryParam("searchParam", "NIGHT_CASHIER")
                .get("/roles/tenant")
                .then().log().all()
                .statusCode(200)
                .body("totalElements", equalTo(1))
                .body("content[0].name", equalTo("NIGHT_CASHIER"))
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
                .get("/roles")
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
                .get("/roles")
                .then().log().all()
                .statusCode(404);

        given()
                .auth().oauth2(token)
                .get("/roles/tenant")
                .then().log().all()
                .statusCode(404);
    }

    @Test
    void listPrivilegeGroupsAttachedToRoleWorks() {
        String email = uniqueEmail("role-groups");
        saveUser(email, "password123");
        String token = login(email, "password123");
        createParentShop(token, "Role Groups Shop");

        String cashierRoleId = given()
                .auth().oauth2(token)
                .queryParam("searchParam", AccessCatalog.CASHIER_ROLE)
                .get("/roles")
                .then()
                .statusCode(200)
                .extract()
                .path("content.find { it.name == '" + AccessCatalog.CASHIER_ROLE + "' }.publicId");

        given()
                .auth().oauth2(token)
                .get("/roles/{rolePublicId}/privilege-groups", cashierRoleId)
                .then().log().all()
                .statusCode(200)
                .body("content.name", hasItems("POS", "SALES", "CUSTOMERS", "DASHBOARD"))
                .body("content.name", not(hasItem("INVENTORY")));
    }

    @Test
    void assignPrivilegeGroupsToRoleThenListThem() {
        String email = uniqueEmail("role-assign-groups");
        saveUser(email, "password123");
        String token = login(email, "password123");
        createParentShop(token, "Assign Groups Shop");

        String roleId = given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(CreateRoleDTO.builder()
                        .name("SHIFT_LEAD")
                        .description("Custom role")
                        .build())
                .post("/roles")
                .then()
                .statusCode(200)
                .extract()
                .path("publicId");

        String salesGroupId = given()
                .auth().oauth2(token)
                .queryParam("searchParam", "SALES")
                .get("/privilege-groups")
                .then()
                .statusCode(200)
                .extract()
                .path("content.find { it.name == 'SALES' }.publicId");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(new RolePrivilegeGroupDto(List.of(UUID.fromString(salesGroupId))))
                .patch("/roles/{rolePublicId}/privilege-groups", roleId)
                .then()
                .statusCode(200)
                .body("identity", containsString("SALES"));

        given()
                .auth().oauth2(token)
                .get("/roles/{rolePublicId}/privilege-groups", roleId)
                .then().log().all()
                .statusCode(200)
                .body("content.name", contains("SALES"));
    }
}
