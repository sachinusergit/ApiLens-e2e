package com.apilens.tests;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.*;

public class ProfileEmailChangeTest extends BaseTest {

    private static final String AUTH_DB_URL = System.getenv().getOrDefault(
            "AUTH_DB_URL", "jdbc:postgresql://localhost:5433/apilens_auth");
    private static final String AUTH_DB_USER = System.getenv().getOrDefault("AUTH_DB_USER", "apilens");
    private static final String AUTH_DB_PASSWORD = System.getenv().getOrDefault("AUTH_DB_PASSWORD", "apilens_dev");

    private static String email;
    private static String newEmail;
    private static String token;
    private static String userId;

    @Test(priority = 1)
    public void registerFreshUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        email = "profile-" + suffix + "@example.com";
        newEmail = "profile-changed-" + suffix + "@example.com";

        Response response = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email":"%s",
                          "password":"Passw0rd!23",
                          "name":"Profile Test User",
                          "orgName":"Profile Test Org"
                        }
                        """.formatted(email))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .extract().response();

        token = response.path("accessToken");
        userId = response.path("userId");
        assertNotNull(token, "registration should return a token");
        assertNotNull(userId, "registration should return a userId");
    }

    @Test(priority = 2, dependsOnMethods = "registerFreshUser")
    public void profileReturnsDetailsAndRateLimitState() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/auth/profile")
                .then()
                .statusCode(200)
                .body("id", equalTo(userId))
                .body("email", equalTo(email))
                .body("name", equalTo("Profile Test User"))
                .body("orgName", equalTo("Profile Test Org"))
                .body("role", equalTo("OWNER"))
                .body("createdAt", notNullValue())
                .body("emailChange.allowed", equalTo(true))
                .body("emailChange.currentCount", equalTo(0))
                .body("emailChange.maxChanges", equalTo(1))
                .body("emailChange.resetInSeconds", greaterThan(0));
    }

    @Test(priority = 3, dependsOnMethods = "registerFreshUser")
    public void firstEmailChangeSucceeds() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {"newEmail":"%s"}
                        """.formatted(newEmail))
                .when()
                .post("/api/auth/change-email")
                .then()
                .statusCode(200)
                .body("oldEmail", equalTo(email))
                .body("newEmail", equalTo(newEmail))
                .body("emailChange.allowed", equalTo(false))
                .body("emailChange.currentCount", equalTo(1))
                .body("emailChange.maxChanges", equalTo(1));
    }

    @Test(priority = 4, dependsOnMethods = "firstEmailChangeSucceeds")
    public void secondEmailChangeReturns429WithRetryAfter() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {"newEmail":"profile-blocked-%s@example.com"}
                        """.formatted(UUID.randomUUID().toString().substring(0, 8)))
                .when()
                .post("/api/auth/change-email")
                .then()
                .statusCode(429)
                .header("Retry-After", notNullValue())
                .body("error", equalTo("rate_limit_exceeded"))
                .body("status", equalTo(429))
                .body("retryAfterSeconds", greaterThan(0))
                .body("resetInSeconds", greaterThan(0));
    }

    @Test(priority = 5, dependsOnMethods = "firstEmailChangeSucceeds")
    public void databaseShowsNewEmailAndHistory() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                AUTH_DB_URL, AUTH_DB_USER, AUTH_DB_PASSWORD)) {

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT email FROM users WHERE id = ?::uuid")) {
                statement.setString(1, userId);
                try (ResultSet rs = statement.executeQuery()) {
                    assertTrue(rs.next(), "user row should exist");
                    assertEquals(rs.getString("email"), newEmail);
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM email_change_history " +
                            "WHERE user_id = ?::uuid AND old_email = ? AND new_email = ?")) {
                statement.setString(1, userId);
                statement.setString(2, email);
                statement.setString(3, newEmail);
                try (ResultSet rs = statement.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(rs.getInt(1), 1, "exactly one history row expected");
                }
            }
        }
    }

    @Test(priority = 6, dependsOnMethods = "firstEmailChangeSucceeds")
    public void redisCounterIsOne() {
        String value = redisCli("GET", "email_change:" + userId).trim();
        assertEquals(value, "1");
    }

    @Test(priority = 7, dependsOnMethods = "firstEmailChangeSucceeds")
    public void redisCounterExpiresIn24Hours() {
        long ttl = Long.parseLong(redisCli("TTL", "email_change:" + userId).trim());
        assertTrue(ttl > 86000 && ttl <= 86400,
                "expected ~86400s TTL, got " + ttl);
    }

    @AfterClass(alwaysRun = true)
    public void cleanup() {
        if (userId == null) return;

        try (Connection connection = DriverManager.getConnection(
                AUTH_DB_URL, AUTH_DB_USER, AUTH_DB_PASSWORD)) {

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM email_change_history WHERE user_id = ?::uuid")) {
                statement.setString(1, userId);
                statement.executeUpdate();
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM users WHERE id = ?::uuid")) {
                statement.setString(1, userId);
                statement.executeUpdate();
            }
        } catch (Exception ignored) {
            // best-effort cleanup
        }

        try {
            dockerQuiet("redis-cli", "DEL", "email_change:" + userId);
        } catch (Exception ignored) {
            // best-effort cleanup
        }
    }

    private static String redisCli(String... args) {
        try {
            List<String> command = new ArrayList<>();
            command.add("docker");
            command.add("exec");
            command.add(System.getenv().getOrDefault("REDIS_CONTAINER", "apilens-redis"));
            command.add("redis-cli");
            command.addAll(Arrays.asList(args));

            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished || process.exitValue() != 0) {
                throw new SkipException("docker/redis-cli unavailable: " + output);
            }
            return output;
        } catch (SkipException e) {
            throw e;
        } catch (Exception e) {
            throw new SkipException("docker/redis-cli unavailable", e);
        }
    }

    private static void dockerQuiet(String... args) {
        try {
            List<String> command = new ArrayList<>();
            command.add("docker");
            command.add("exec");
            command.add(System.getenv().getOrDefault("REDIS_CONTAINER", "apilens-redis"));
            command.add("redis-cli");
            command.addAll(Arrays.asList(args));
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            process.waitFor(10, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // best-effort
        }
    }
}
