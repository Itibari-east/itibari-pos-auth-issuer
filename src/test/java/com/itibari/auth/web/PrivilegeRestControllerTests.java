package com.itibari.auth.web;

import com.itibari.auth.AuthIssuerApplicationTests;
import com.itibari.auth.model.Privilege;
import com.itibari.auth.model.PrivilegeGroup;
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
                .queryParam("size", 100)
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
        createParentShop(token, "Tenant Shop");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(new PrivilegeGroupDto("Warehouse Docs", null))
                .post("/privilege-groups")
                .then().log().all()
                .statusCode(200)
                .body("message", equalTo("successfully created privilege group"))
                .body("identity", equalTo("Warehouse Docs"));

        String publicId = given()
                .auth().oauth2(token)
                .queryParam("searchParam", "Warehouse Docs")
                .get("/privilege-groups")
                .then().log().all()
                .statusCode(200)
                .body("content.name", hasItem("Warehouse Docs"))
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

        given()
                .auth().oauth2(token)
                .get("/privilege-groups/{groupPublicId}/privileges", publicId)
                .then().log().all()
                .statusCode(200)
                .body("content.name", hasItem("EDIT_PRODUCT"))
                .body("content.publicId", hasItem(privilege.getPublicId().toString()));
    }

    @Test
    void listPrivilegesForCatalogGroupWorks() {
        String email = uniqueEmail("priv-group-catalog");
        saveUser(email, "password123");
        String token = login(email, "password123");
        createParentShop(token, "Catalog Group Shop");

        String salesGroupId = given()
                .auth().oauth2(token)
                .queryParam("searchParam", "SALES")
                .get("/privilege-groups")
                .then()
                .statusCode(200)
                .body("content.name", hasItem("SALES"))
                .extract()
                .path("content.find { it.name == 'SALES' }.publicId");

        given()
                .auth().oauth2(token)
                .get("/privilege-groups/{groupPublicId}/privileges", salesGroupId)
                .then().log().all()
                .statusCode(200)
                .body("content.name", hasItems("VIEW_SALES", "CREATE_INVOICE", "CREATE_QUOTATION",
                        "CREATE_PROFORMA", "VIEW_RETURNS"));
    }

    @Test
    void createPrivilegeGroupWithoutTenantIdIsListedForLoggedInShop() {
        String email = uniqueEmail("priv-group-ui-create");
        saveUser(email, "password123");
        String token = login(email, "password123");
        createParentShop(token, "Tenant Shop");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(new PrivilegeGroupDto("UI Inventory", null))
                .post("/privilege-groups")
                .then().log().all()
                .statusCode(200)
                .body("identity", equalTo("UI Inventory"));

        given()
                .auth().oauth2(token)
                .get("/privilege-groups")
                .then().log().all()
                .statusCode(200)
                .body("content.name", hasItem("UI Inventory"));
    }

    @Test
    void listTenantPrivilegeGroupsReturnsOnlyParentTenantGroups() {
        String email = uniqueEmail("priv-group-tenant");
        saveUser(email, "password123");
        String token = login(email, "password123");
        UUID tenantId = UUID.fromString(createParentShop(token, "Tenant Shop"));

        PrivilegeGroup tenantGroup = savePrivilegeGroup("MODULE_VIEWS", tenantId);
        savePrivilegeGroup("OTHER_TENANT_GROUP", UUID.randomUUID());
        savePrivilegeGroup("GLOBAL_GROUP", null);

        given()
                .auth().oauth2(token)
                .queryParam("searchParam", "MODULE_VIEWS")
                .get("/privilege-groups/tenant")
                .then().log().all()
                .statusCode(200)
                .body("totalElements", equalTo(1))
                .body("content[0].name", equalTo("MODULE_VIEWS"))
                .body("content[0].publicId", equalTo(tenantGroup.getPublicId().toString()));
    }

    @Test
    void listTenantPrivilegeGroupsRequiresParentShop() {
        String email = uniqueEmail("priv-group-no-shop");
        saveUser(email, "password123");
        String token = login(email, "password123");

        given()
                .auth().oauth2(token)
                .get("/privilege-groups")
                .then().log().all()
                .statusCode(404);

        given()
                .auth().oauth2(token)
                .get("/privilege-groups/tenant")
                .then().log().all()
                .statusCode(404);
    }
}
