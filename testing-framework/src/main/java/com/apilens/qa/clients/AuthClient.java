package com.apilens.qa.clients;

import com.apilens.qa.config.TestConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * Auth endpoints (via gateway):
 *   POST /api/auth/login     -> 200 | 400 (bad credentials)
 *   POST /api/auth/register  -> 201 | 400 (duplicate email)
 *   GET  /api/auth/me        -> 200 | 401 (no/invalid token)
 *   GET  /api/auth/profile   -> 200 (includes emailChange rate-limit state)
 */
public class AuthClient {

    public Response login(String email, String password) {
        return given()
                .baseUri(TestConfig.BASE_URL)
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password))
                .when()
                .post("/api/auth/login");
    }

    public Response register(String email, String password, String name, String orgName) {
        return given()
                .baseUri(TestConfig.BASE_URL)
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"%s","password":"%s","name":"%s","orgName":"%s"}
                        """.formatted(email, password, name, orgName))
                .when()
                .post("/api/auth/register");
    }

    public Response me(String token) {
        return given()
                .baseUri(TestConfig.BASE_URL)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/auth/me");
    }

    public Response profile(String token) {
        return given()
                .baseUri(TestConfig.BASE_URL)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/auth/profile");
    }
}
