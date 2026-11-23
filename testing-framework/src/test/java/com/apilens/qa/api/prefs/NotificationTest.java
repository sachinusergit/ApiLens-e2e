package com.apilens.qa.api.prefs;

import com.apilens.qa.api.BaseParallelApiTest;
import com.apilens.qa.clients.PrefsClient;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

public class NotificationTest extends BaseParallelApiTest {

    private final PrefsClient prefsClient = new PrefsClient();

    @Test(priority = 1)
    public void notification_validType_returns201() {
        Response r = prefsClient.notification(token(), "info", "Test notification");
        r.then()
                .statusCode(201)
                .body("type", equalTo("info"));
    }

    @Test(priority = 2)
    public void banners_active_returns200AndList() {
        Response r = prefsClient.activeBanners(token());
        r.then()
                .statusCode(200)
                .body("$", instanceOf(List.class));
    }
}
