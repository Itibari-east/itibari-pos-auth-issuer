package com.itibari.auth.web;

import com.itibari.auth.AuthIssuerApplicationTests;
import com.itibari.auth.model.Country;
import com.itibari.auth.repository.CountryRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CountryRestControllerTests extends AuthIssuerApplicationTests {

    @Autowired
    private CountryRepository countryRepository;

    private UUID tanzaniaPublicId;
    private UUID kenyaPublicId;

    @BeforeEach
    void seedCountries() {
        seed("Burundi", "BDI", "+257");
        seed("Ethiopia", "ETH", "+251");
        kenyaPublicId = seed("Kenya", "KEN", "+254");
        seed("Madagascar", "MDG", "+261");
        tanzaniaPublicId = seed("Tanzania", "TZA", "+255");
        seed("Uganda", "UGA", "+256");
    }

    @Test
    void missingBearerTokenIsUnauthorized() {
        given().get("/countries").then().statusCode(401);
        given().contentType(ContentType.JSON).body("{\"isoAlpha\":\"KEN\"}").post("/countries").then().statusCode(401);
        given().get("/countries/currencies").then().statusCode(401);
    }

    @Test
    void blankSearchReturnsOnlyLiveRowsAndDoesNotInsert() {
        String token = loginToken();
        long before = countryCount();

        given()
                .auth().oauth2(token)
                .queryParam("size", 250)
                .get("/countries")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(6))
                .body("content.find { it.isoAlpha == 'TZA' }.name", equalTo("Tanzania"))
                .body("content.find { it.isoAlpha == 'DEU' }", nullValue());

        assertEquals(before, countryCount());
    }

    @Test
    void searchKenyaReturnsStoredRowAndDoesNotInsert() {
        String token = loginToken();
        long before = countryCount();

        given()
                .auth().oauth2(token)
                .queryParam("searchParam", "Kenya")
                .get("/countries")
                .then()
                .statusCode(200)
                .body("content.find { it.isoAlpha == 'KEN' }.name", equalTo("Kenya"))
                .body("content.find { it.isoAlpha == 'KEN' }.countryCode", equalTo("+254"))
                .body("content.find { it.isoAlpha == 'KEN' }.isoAlphaTwo", equalTo("KE"))
                .body("content.find { it.isoAlpha == 'KEN' }.currencyCode", equalTo("KES"))
                .body("content.find { it.isoAlpha == 'KEN' }.publicId", equalTo(kenyaPublicId.toString()));

        assertEquals(before, countryCount());
    }

    @Test
    void searchTanzaniaReturnsOneStoredRowAndDoesNotInsert() {
        String token = loginToken();
        long before = countryCount();

        given()
                .auth().oauth2(token)
                .queryParam("searchParam", "TZA")
                .get("/countries")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].name", equalTo("Tanzania"))
                .body("content[0].publicId", equalTo(tanzaniaPublicId.toString()));

        assertEquals(before, countryCount());
    }

    @Test
    void searchGermanyReturnsCatalogHitAndDoesNotInsert() {
        String token = loginToken();
        long before = countryCount();

        given()
                .auth().oauth2(token)
                .queryParam("searchParam", "Germany")
                .get("/countries")
                .then()
                .statusCode(200)
                .body("content.find { it.isoAlpha == 'DEU' }.publicId", nullValue())
                .body("content.find { it.isoAlpha == 'DEU' }.name", equalTo("Germany"))
                .body("content.find { it.isoAlpha == 'DEU' }.countryCode", equalTo("+49"))
                .body("content.find { it.isoAlpha == 'DEU' }.isoAlphaTwo", equalTo("DE"))
                .body("content.find { it.isoAlpha == 'DEU' }.currencyCode", equalTo("EUR"));

        assertEquals(before, countryCount());
    }

    @Test
    void postTanzaniaReturnsExistingRow() {
        String token = loginToken();
        long before = countryCount();

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("{\"isoAlpha\":\"TZA\"}")
                .post("/countries")
                .then()
                .statusCode(200)
                .body("publicId", equalTo(tanzaniaPublicId.toString()))
                .body("name", equalTo("Tanzania"))
                .body("isoAlpha", equalTo("TZA"));

        assertEquals(before, countryCount());
    }

    @Test
    void postGermanyInsertsOnce() {
        String token = loginToken();
        long before = countryCount();

        String publicId = given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("{\"isoAlpha\":\"DEU\"}")
                .post("/countries")
                .then()
                .statusCode(201)
                .body("publicId", notNullValue())
                .body("name", equalTo("Germany"))
                .body("isoAlpha", equalTo("DEU"))
                .body("countryCode", equalTo("+49"))
                .body("isoAlphaTwo", equalTo("DE"))
                .body("currencyCode", equalTo("EUR"))
                .extract()
                .path("publicId");

        assertEquals(before + 1, countryCount());

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("{\"isoAlpha\":\"DEU\"}")
                .post("/countries")
                .then()
                .statusCode(200)
                .body("publicId", equalTo(publicId));

        assertEquals(before + 1, countryCount());
    }

    @Test
    void postUnknownAlpha3IsBadRequestAndInsertsNothing() {
        String token = loginToken();
        long before = countryCount();

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("{\"isoAlpha\":\"ZZZ\"}")
                .post("/countries")
                .then()
                .statusCode(400);

        assertEquals(before, countryCount());
    }

    @Test
    void searchCurrenciesReturnsTanzanianShillingAndDoesNotInsert() {
        String token = loginToken();
        long before = countryCount();

        given()
                .auth().oauth2(token)
                .queryParam("searchParam", "TZS")
                .get("/countries/currencies")
                .then()
                .statusCode(200)
                .body("content.find { it.code == 'TZS' }.name", equalTo("Tanzanian Shilling"))
                .body("content.find { it.code == 'TZS' }.fractionDigits", equalTo(2));

        assertEquals(before, countryCount());
    }

    @Test
    void internalGetCountryReturnsExistingFields() {
        String token = loginToken();

        given()
                .auth().oauth2(token)
                .get("/internal/countries/{publicId}", tanzaniaPublicId)
                .then()
                .statusCode(200)
                .body("publicId", equalTo(tanzaniaPublicId.toString()))
                .body("name", equalTo("Tanzania"))
                .body("isoAlpha", equalTo("TZA"))
                .body("countryCode", equalTo("+255"));
    }

    private String loginToken() {
        String email = uniqueEmail("country");
        saveUser(email, "password123");
        return login(email, "password123");
    }

    private UUID seed(String name, String isoAlphaThree, String phoneCode) {
        Country country = new Country();
        country.setName(name);
        country.setIsoAlphaThree(isoAlphaThree);
        country.setPhoneCode(phoneCode);
        return countryRepository.saveAndFlush(country).getPublicId();
    }

    private long countryCount() {
        return countryRepository.count();
    }
}
