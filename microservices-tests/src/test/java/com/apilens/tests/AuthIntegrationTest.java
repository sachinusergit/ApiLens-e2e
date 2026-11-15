package com.apilens.tests;

import io.restassured.http.ContentType;
import org.testng.annotations.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class AuthIntegrationTest extends BaseTest {

    @Test(priority = 1)
    public void login_demoUser_returnsToken() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"ravi@example.com","password":"Test@1234"}
                        """)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("expiresIn", greaterThan(0))
                .body("role", equalTo("OWNER"));
    }

    @Test(priority = 2)
    public void login_wrongPassword_returns4xx() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"ravi@example.com","password":"WrongPass"}
                        """)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(401)));
    }

    @Test(priority = 3)
    public void register_newUser_returns201() {
        String email = "test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email":"%s",
                          "password":"Passw0rd!23",
                          "name":"Test User",
                          "orgName":"Test Org"
                        }
                        """.formatted(email))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .body("accessToken", notNullValue())
                .body("email", equalTo(email));
    }

    @Test(priority = 4)
    public void me_withToken_returnsProfile() {
        String token = loginAsDemoUser();
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/auth/me")
                .then()
                .statusCode(200)
                .body("email", equalTo("ravi@example.com"))
                .body("role", equalTo("OWNER"));
    }

    @Test(priority = 5)
    public void me_withoutToken_returns401() {
        given()
                .when()
                .get("/api/auth/me")
                .then()
                .statusCode(401);
    }
}
