package com.apilens.tests;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;

import static io.restassured.RestAssured.given;

public class BaseTest {

    protected static final String BASE_URL = System.getenv().getOrDefault("BASE_URL", "http://localhost:8080");
    protected static String adminToken;

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    protected String loginAsDemoUser() {
        Response r = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email": "ravi@example.com",
                          "password": "Test@1234"
                        }
                        """)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract().response();
        return r.path("accessToken");
    }
}
