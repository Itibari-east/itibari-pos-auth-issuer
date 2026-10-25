package com.itibari.auth.web;

import com.itibari.auth.AuthIssuerApplicationTests;
import com.itibari.auth.access.AccessCatalog;
import com.itibari.auth.model.AuthUser;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class AccessCatalogTests extends AuthIssuerApplicationTests {

    @Test
    void globalPrivilegeCatalogIsSeeded() {
        String email = uniqueEmail("catalog");
        saveUser(email, "password123");
        String token = login(email, "password123");

        given()
                .auth().oauth2(token)
                .queryParam("size", 100)
                .get("/privileges")
                .then()
                .statusCode(200)
                .body("content.name", hasItems("VIEW_DASHBOARD", "VIEW_POS", "VIEW_SALES", "VIEW_INVENTORY",
                        "VIEW_USERS", "MANAGE_ROLES"));
    }

    @Test
    void creatingShopSeedsBasicRolesAndAssignsOwner() {
        String email = uniqueEmail("catalog-owner");
        AuthUser user = saveUser(email, "password123");
        String token = login(email, "password123");
        createParentShop(token, "Catalog Shop");

        given()
                .auth().oauth2(token)
                .queryParam("size", 20)
                .get("/roles")
                .then()
                .statusCode(200)
                .body("content.name", hasItems(
                        AccessCatalog.OWNER_ROLE,
                        AccessCatalog.CASHIER_ROLE,
                        AccessCatalog.STOREKEEPER_ROLE,
                        AccessCatalog.ACCOUNTANT_ROLE,
                        AccessCatalog.HR_ROLE));

        given()
                .auth().oauth2(token)
                .get("/users/{userId}/roles", user.getPublicId())
                .then()
                .statusCode(200)
                .body("content.name", hasItem(AccessCatalog.OWNER_ROLE));
    }

    @Test
    void customRoleCanStillBeCreatedFromUi() {
        String email = uniqueEmail("catalog-custom");
        saveUser(email, "password123");
        String token = login(email, "password123");
        createParentShop(token, "Custom Role Shop");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Supervisor\",\"description\":\"Custom shop role\"}")
                .post("/roles")
                .then()
                .statusCode(200)
                .body("name", equalTo("Supervisor"));

        given()
                .auth().oauth2(token)
                .get("/roles")
                .then()
                .statusCode(200)
                .body("content.name", hasItems(AccessCatalog.OWNER_ROLE, "Supervisor"));
    }

    @Test
    void tenantPrivilegeGroupListIncludesGlobalCatalog() {
        String email = uniqueEmail("catalog-groups");
        saveUser(email, "password123");
        String token = login(email, "password123");
        createParentShop(token, "Group Shop");

        given()
                .auth().oauth2(token)
                .queryParam("size", 50)
                .get("/privilege-groups")
                .then()
                .statusCode(200)
                .body("content.name", hasItems("SALES", "POS", "INVENTORY", "ACCOUNTING", "STAFF"));
    }
}
