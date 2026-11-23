package com.apilens.qa.api.auth;

import com.apilens.qa.api.BaseParallelApiTest;
import com.apilens.qa.fixtures.UserFactory;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.anyOf;

public class SignupTest extends BaseParallelApiTest {

    @Test(priority = 1)
    public void signup_newUser_returns201() {
        String email = UserFactory.uniqueEmail();
        Response r = authClient.register(email, "Test@1234", "New User", "New Org");
        r.then()
                .statusCode(201)
                .body("accessToken", notNullValue())
                .body("email", equalTo(email))
                .body("role", equalTo("OWNER"));
    }

    @Test(priority = 2)
    public void signup_duplicateEmail_returns400() {
        String email = UserFactory.uniqueEmail();
        authClient.register(email, "Test@1234", "First", "Org").then().statusCode(201);

        Response r = authClient.register(email, "Test@1234", "Second", "Org");
        r.then().statusCode(anyOf(equalTo(400), equalTo(409)));
    }
}
