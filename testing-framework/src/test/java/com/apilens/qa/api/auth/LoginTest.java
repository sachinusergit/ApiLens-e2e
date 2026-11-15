package com.apilens.qa.api.auth;

import com.apilens.qa.api.BaseParallelApiTest;
import com.apilens.qa.config.TestConfig;
import com.apilens.qa.fixtures.UserFactory;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.anyOf;

public class LoginTest extends BaseParallelApiTest {

    @Test(priority = 1)
    public void login_validCredentials_returnsToken() {
        Response r = authClient.login(email(), UserFactory.defaultPassword());
        r.then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("expiresIn", greaterThan(0))
                .body("role", equalTo("OWNER"));
    }

    @Test(priority = 2)
    public void login_wrongPassword_returns400() {
        Response r = authClient.login(email(), "WrongPassword1");
        r.then().statusCode(anyOf(equalTo(400), equalTo(401)));
    }

    @Test(priority = 3)
    public void login_missingPassword_returns400() {
        Response r = given()
                .baseUri(TestConfig.BASE_URL)
                .contentType("application/json")
                .body("{\"email\":\"" + email() + "\"}")
                .when()
                .post("/api/auth/login");
        r.then().statusCode(anyOf(equalTo(400), equalTo(422)));
    }
}
