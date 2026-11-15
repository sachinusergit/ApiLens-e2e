package com.apilens.tests;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * End-to-end: register → check DB → login → check /me, plus bulk creation.
 * Proves each newly registered user exists in Postgres and can log in.
 */
public class SignupLoginFlowTest extends BaseTest {

    private static final String AUTH_DB_URL = System.getenv().getOrDefault(
            "AUTH_DB_URL", "jdbc:postgresql://localhost:5433/apilens_auth");

    @Test(priority = 1)
    public void fullSignupLoginFlow() throws Exception {
        String uniqueEmail = "signup_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String password = "TestPass@1234";
        String name = "Signup Test User";

        Response registerResp =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "name": "%s",
                                  "orgName": "Signup Org"
                                }
                                """.formatted(uniqueEmail, password, name))
                .when()
                        .post("/api/auth/register")
                .then()
                        .statusCode(201)
                        .body("accessToken", notNullValue())
                        .body("email", equalTo(uniqueEmail))
                        .body("role", equalTo("OWNER"))
                        .extract().response();

        String registeredUserId = registerResp.path("userId");
        String registeredOrgId = registerResp.path("orgId");

        assertTrue(registeredUserId != null, "userId should be present");
        assertTrue(registeredOrgId != null, "orgId should be present");

        try (Connection c = DriverManager.getConnection(AUTH_DB_URL, "apilens", "apilens_dev");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT email, name, role, org_id FROM users WHERE id = '" + registeredUserId + "'")) {

            assertTrue(rs.next(), "User row should exist in DB after registration");
            assertEquals(rs.getString("email"), uniqueEmail, "Email in DB should match");
            assertEquals(rs.getString("name"), name, "Name in DB should match");
            assertEquals(rs.getString("role"), "OWNER", "Role should be OWNER");
            assertEquals(rs.getString("org_id"), registeredOrgId, "Org ID should match");
        }

        Response loginResp =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(uniqueEmail, password))
                .when()
                        .post("/api/auth/login")
                .then()
                        .statusCode(200)
                        .body("accessToken", notNullValue())
                        .body("userId", equalTo(registeredUserId))
                        .extract().response();

        String loginToken = loginResp.path("accessToken");

        given()
                .header("Authorization", "Bearer " + loginToken)
        .when()
                .get("/api/auth/me")
        .then()
                .statusCode(200)
                .body("email", equalTo(uniqueEmail))
                .body("id", equalTo(registeredUserId))
                .body("role", equalTo("OWNER"));

        System.out.println("Full signup -> DB -> login -> /me passed for: " + uniqueEmail);
    }

    @Test(priority = 2)
    public void duplicateSignupRejected() {
        String email = "dupe_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"%s","password":"TestPass@1234","name":"Dupe User","orgName":"Org"}
                        """.formatted(email))
                .when()
                        .post("/api/auth/register")
                .then()
                        .statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"%s","password":"TestPass@1234","name":"Dupe User","orgName":"Org"}
                        """.formatted(email))
                .when()
                        .post("/api/auth/register")
                .then()
                        .statusCode(anyOf(equalTo(400), equalTo(409)));
    }

    @Test(priority = 3)
    public void bulkCreate_thenEachUserCanLogin() throws Exception {
        String token = loginAsDemoUser();
        String prefix = "bulk_" + UUID.randomUUID().toString().substring(0, 6) + "_";
        String password = "BulkPass@1234";
        int count = 3;

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                        {"count":%d,"emailPrefix":"%s","emailDomain":"example.com","password":"%s"}
                        """.formatted(count, prefix, password))
                .when()
                        .post("/api/auth/admin/users/bulk")
                .then()
                        .statusCode(200)
                        .body("created", equalTo(count));

        try (Connection c = DriverManager.getConnection(AUTH_DB_URL, "apilens", "apilens_dev");
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM users WHERE email LIKE ?")) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(rs.getInt(1), count, "All bulk users should be persisted");
            }
        }

        for (int i = 0; i < count; i++) {
            String email = prefix + i + "@example.com";
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                            {"email":"%s","password":"%s"}
                            """.formatted(email, password))
                    .when()
                            .post("/api/auth/login")
                    .then()
                            .statusCode(200)
                            .body("accessToken", notNullValue())
                    .body("email", equalTo(email));
        }

        System.out.println("Bulk-created " + count + " users; each can log in: " + prefix + "*");
    }
}
