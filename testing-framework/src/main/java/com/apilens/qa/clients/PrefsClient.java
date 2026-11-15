package com.apilens.qa.clients;

import com.apilens.qa.config.TestConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * Preferences service endpoints (via gateway):
 *   POST /api/preferences/slider  -> 200 (upsert, value 0-100)
 *   POST /api/confirmations       -> 200 (choice: YES|NO)
 *   POST /api/notifications       -> 201 (type: success|error|info|warning)
 *   GET  /api/banners/active      -> 200 (requires JWT; not a public path)
 */
public class PrefsClient {

    public Response saveSlider(String token, String key, int value) {
        return given()
                .baseUri(TestConfig.BASE_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                        {"key":"%s","value":%d}
                        """.formatted(key, value))
                .when()
                .post("/api/preferences/slider");
    }

    public Response confirmation(String token, String action, String choice, String note) {
        return given()
                .baseUri(TestConfig.BASE_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                        {"action":"%s","choice":"%s","note":"%s"}
                        """.formatted(action, choice, note))
                .when()
                .post("/api/confirmations");
    }

    public Response notification(String token, String type, String message) {
        return given()
                .baseUri(TestConfig.BASE_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                        {"type":"%s","message":"%s"}
                        """.formatted(type, message))
                .when()
                .post("/api/notifications");
    }

    public Response activeBanners(String token) {
        return given()
                .baseUri(TestConfig.BASE_URL)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/banners/active");
    }
}
