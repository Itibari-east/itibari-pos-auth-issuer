package com.itibari.auth.web;

import com.itibari.auth.AuthIssuerApplicationTests;
import com.itibari.auth.dataTypes.SubscriptionType;
import com.itibari.auth.model.request.CreateProfileDTO;
import com.itibari.auth.model.request.CreateSubscriptionDTO;
import com.itibari.auth.repository.SubscriptionRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class SubscriptionRestControllerTests extends AuthIssuerApplicationTests {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    void listSubscriptionPlansWorks() {
        String email = uniqueEmail("sub-list");
        saveUser(email, "password123");
        String token = login(email, "password123");

        given()
                .auth().oauth2(token)
                .get("/subscription")
                .then().log().all()
                .statusCode(200)
                .body("totalElements", greaterThanOrEqualTo(1))
                .body("content.name", hasItem(containsString("Subscription")));
    }

    @Test
    void createSubscriptionPlanWorks() {
        String email = uniqueEmail("sub-create");
        saveUser(email, "password123");
        String token = login(email, "password123");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(CreateSubscriptionDTO.builder()
                        .name(SubscriptionType.Single_Shop)
                        .description("Updated single shop")
                        .price(new BigDecimal("75000.00"))
                        .trialDays(14)
                        .days(30)
                        .build())
                .post("/subscription")
                .then().log().all()
                .statusCode(200)
                .body("message", equalTo("success"))
                .body("identity", equalTo(SubscriptionType.Single_Shop.getLabel()));
    }

    @Test
    void freeTrialUpdatesTokenSubscriptionClaim() {
        String email = uniqueEmail("trial");
        saveUser(email, "password123");
        String token = login(email, "password123");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(CreateProfileDTO.builder().name("Trial Shop").country("TZ").build())
                .post("/institutions")
                .then()
                .statusCode(200);

        String ownerToken = login(email, "password123");
        String planPublicId = subscriptionRepository.findByNameAndDeletedIsFalse(SubscriptionType.Single_Shop)
                .orElseThrow()
                .getPublicId()
                .toString();

        given()
                .auth().oauth2(ownerToken)
                .post("/institutions/subscriptions/{subscriptionId}/free-trial", planPublicId)
                .then().log().all()
                .statusCode(200)
                .body("accessToken", not(blankOrNullString()))
                .body("subscription", equalTo(true));
    }
}
