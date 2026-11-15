package com.apilens.qa.api;

import com.apilens.qa.clients.AuthClient;
import com.apilens.qa.fixtures.UserFactory;
import com.apilens.qa.utils.ThreadContext;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Parallel-safe base: each test class registers its own fresh user
 * (fresh org too, so search/list assertions are org-isolated).
 */
public abstract class BaseParallelApiTest {

    protected final AuthClient authClient = new AuthClient();

    @BeforeClass(alwaysRun = true)
    public void setupUser() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        String email = UserFactory.uniqueEmail();
        String password = UserFactory.defaultPassword();
        String name = UserFactory.uniqueName();

        Response r = authClient.register(email, password, name, "Test Org");
        if (r.statusCode() != 201) {
            throw new RuntimeException("Failed to register test user: " + r.body().asString());
        }

        ThreadContext.setAuth(
                r.path("accessToken"),
                r.path("userId"),
                r.path("orgId"),
                email
        );

        System.out.println("[" + getClass().getSimpleName() + "] Fresh user: " + email);
    }

    @AfterClass(alwaysRun = true)
    public void cleanup() {
        ThreadContext.clear();
    }

    protected String token() { return ThreadContext.getToken(); }
    protected String userId() { return ThreadContext.getUserId(); }
    protected String orgId() { return ThreadContext.getOrgId(); }
    protected String email() { return ThreadContext.getEmail(); }
}
