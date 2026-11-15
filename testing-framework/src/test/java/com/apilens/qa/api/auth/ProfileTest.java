package com.apilens.qa.api.auth;

import com.apilens.qa.api.BaseParallelApiTest;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

public class ProfileTest extends BaseParallelApiTest {

    @Test(priority = 1)
    public void me_withToken_returnsProfile() {
        Response r = authClient.me(token());
        r.then()
                .statusCode(200)
                .body("email", equalTo(email()))
                .body("id", equalTo(userId()));
    }

    @Test(priority = 2)
    public void profile_withToken_returnsRateLimitInfo() {
        Response r = authClient.profile(token());
        r.then()
                .statusCode(200)
                .body("emailChange.allowed", notNullValue())
                .body("emailChange.maxChanges", greaterThan(0));
    }
}
