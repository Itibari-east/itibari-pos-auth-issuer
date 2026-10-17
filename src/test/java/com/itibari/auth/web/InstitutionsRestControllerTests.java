package com.itibari.auth.web;

import com.itibari.auth.AuthIssuerApplicationTests;
import com.itibari.auth.dataTypes.SubscriptionType;
import com.itibari.auth.model.request.CreateChildShopDTO;
import com.itibari.auth.model.request.CreateProfileDTO;
import com.itibari.auth.model.request.UpdateShopDTO;
import com.itibari.auth.repository.SubscriptionRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class InstitutionsRestControllerTests extends AuthIssuerApplicationTests {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    void createProfileReturnsTokenWithActiveShop() {
        String email = uniqueEmail("shop-owner");
        saveUser(email, "password123");
        String token = login(email, "password123");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(CreateProfileDTO.builder()
                        .name("Acme Retail")
                        .country("TZ")
                        .city("Dar")
                        .baseCurrency("TZS")
                        .timeZone("Africa/Dar_es_Salaam")
                        .build())
                .post("/institutions")
                .then().log().all()
                .statusCode(200)
                .body("message", equalTo("success"))
                .body("identity", equalTo("Acme Retail"))
                .body("publicId", not(blankOrNullString()))
                .body("accessToken", not(blankOrNullString()));
    }

    @Test
    void childShopMyShopsAndSwitchShopWork() {
        String email = uniqueEmail("multi-shop");
        saveUser(email, "password123");
        String token = login(email, "password123");

        String parentPublicId = given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(CreateProfileDTO.builder()
                        .name("Parent Shop")
                        .country("TZ")
                        .city("Arusha")
                        .build())
                .post("/institutions")
                .then()
                .statusCode(200)
                .extract()
                .path("publicId");

        String ownerToken = login(email, "password123");
        String enterprisePlanId = subscriptionRepository.findByNameAndDeletedIsFalse(SubscriptionType.Enterprise)
                .orElseThrow()
                .getPublicId()
                .toString();

        ownerToken = given()
                .auth().oauth2(ownerToken)
                .post("/institutions/subscriptions/{subscriptionId}/free-trial", enterprisePlanId)
                .then()
                .statusCode(200)
                .extract()
                .path("accessToken");

        given()
                .auth().oauth2(ownerToken)
                .contentType(ContentType.JSON)
                .body(new CreateChildShopDTO("Child Branch", "TZ", null, null, "TZS", "Moshi"))
                .post("/institutions/{parentTenantId}/childShop", parentPublicId)
                .then().log().all()
                .statusCode(200)
                .body("identity", equalTo("Child Branch"));

        given()
                .auth().oauth2(ownerToken)
                .get("/institutions/myShops/summary")
                .then().log().all()
                .statusCode(200)
                .body("name", hasItems("Parent Shop", "Child Branch"));

        String childPublicId = given()
                .auth().oauth2(ownerToken)
                .get("/institutions/myShops")
                .then()
                .statusCode(200)
                .extract()
                .path("find { it.name == 'Child Branch' }.publicId");

        given()
                .auth().oauth2(ownerToken)
                .post("/institutions/{institutionPublicId}/switch", childPublicId)
                .then().log().all()
                .statusCode(200)
                .body("accessToken", not(blankOrNullString()));

        given()
                .auth().oauth2(ownerToken)
                .contentType(ContentType.JSON)
                .body(new UpdateShopDTO("Parent Shop Updated", "TZ", null, null, "TZS", "Arusha"))
                .patch("/institutions/{institutionPublicId}", parentPublicId)
                .then().log().all()
                .statusCode(200)
                .body("identity", equalTo("Parent Shop Updated"));
    }

    @Test
    void listParentProfilesRequiresAuth() {
        given()
                .get("/institutions")
                .then().log().all()
                .statusCode(401);
    }

    @Test
    void createChildShopDeniedForNonMember() {
        String ownerEmail = uniqueEmail("owner");
        String strangerEmail = uniqueEmail("stranger");
        saveUser(ownerEmail, "password123");
        saveUser(strangerEmail, "password123");

        String ownerToken = login(ownerEmail, "password123");
        String strangerToken = login(strangerEmail, "password123");

        String parentPublicId = given()
                .auth().oauth2(ownerToken)
                .contentType(ContentType.JSON)
                .body(CreateProfileDTO.builder().name("Owner Shop").country("TZ").build())
                .post("/institutions")
                .then()
                .statusCode(200)
                .extract()
                .path("publicId");

        given()
                .auth().oauth2(strangerToken)
                .contentType(ContentType.JSON)
                .body(new CreateChildShopDTO("Hack Branch", "TZ", null, null, null, null))
                .post("/institutions/{parentTenantId}/childShop", parentPublicId)
                .then().log().all()
                .statusCode(403);
    }
}
