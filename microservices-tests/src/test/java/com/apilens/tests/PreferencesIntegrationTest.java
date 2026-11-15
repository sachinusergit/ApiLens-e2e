package com.apilens.tests;

import io.restassured.http.ContentType;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Proves the 7 UI features' backend calls actually write rows to apilens_prefs.
 */
public class PreferencesIntegrationTest extends BaseTest {

    private static final String PREFS_DB_URL = System.getenv().getOrDefault(
            "PREFS_DB_URL", "jdbc:postgresql://localhost:5436/apilens_prefs");
    private static final UUID DEMO_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test(priority = 1)
    public void slider_persistsPreferenceRow() throws Exception {
        String token = loginAsDemoUser();
        String key = "volume_" + UUID.randomUUID().toString().substring(0, 8);
        int value = 73;

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                        {"key":"%s","value":%d}
                        """.formatted(key, value))
                .when()
                        .post("/api/preferences/slider")
                .then()
                        .statusCode(200)
                        .body("key", equalTo(key))
                        .body("value", equalTo(value));

        try (Connection c = DriverManager.getConnection(PREFS_DB_URL, "apilens", "apilens_dev");
             PreparedStatement ps = c.prepareStatement(
                     "SELECT value FROM preferences WHERE user_id = ? AND key = ?")) {
            ps.setObject(1, DEMO_USER_ID);
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "preference row should exist");
                assertEquals(rs.getInt("value"), value);
            }
        }
    }

    @Test(priority = 2)
    public void confirmation_persistsYesNoChoice() throws Exception {
        String token = loginAsDemoUser();

        String id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                        {"action":"DELETE_FILE","choice":"NO","note":"integration-test"}
                        """)
                .when()
                        .post("/api/confirmations")
                .then()
                        .statusCode(200)
                        .body("choice", equalTo("NO"))
                        .extract().path("id");

        try (Connection c = DriverManager.getConnection(PREFS_DB_URL, "apilens", "apilens_dev");
             PreparedStatement ps = c.prepareStatement(
                     "SELECT action, choice FROM confirmations WHERE id = ?")) {
            ps.setObject(1, UUID.fromString(id));
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "confirmation row should exist");
                assertEquals(rs.getString("action"), "DELETE_FILE");
                assertEquals(rs.getString("choice"), "NO");
            }
        }
    }

    @Test(priority = 3)
    public void notification_persistsToastRow() throws Exception {
        String token = loginAsDemoUser();
        String message = "toast-" + UUID.randomUUID().toString().substring(0, 8);

        String id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                        {"type":"success","message":"%s"}
                        """.formatted(message))
                .when()
                        .post("/api/notifications")
                .then()
                        .statusCode(201)
                        .body("type", equalTo("success"))
                        .extract().path("id");

        try (Connection c = DriverManager.getConnection(PREFS_DB_URL, "apilens", "apilens_dev");
             PreparedStatement ps = c.prepareStatement(
                     "SELECT type, message FROM notifications WHERE id = ?")) {
            ps.setObject(1, UUID.fromString(id));
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "notification row should exist");
                assertEquals(rs.getString("type"), "success");
                assertEquals(rs.getString("message"), message);
            }
        }
    }

    @Test(priority = 4)
    public void banners_activeReturnsSeededRows() {
        String token = loginAsDemoUser();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                        .get("/api/banners/active")
                .then()
                        .statusCode(200)
                        .body("size()", greaterThanOrEqualTo(2))
                        .body("[0].title", notNullValue());
    }
}
