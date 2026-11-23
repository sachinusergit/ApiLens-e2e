package com.apilens.qa.api.prefs;

import com.apilens.qa.api.BaseParallelApiTest;
import com.apilens.qa.clients.PrefsClient;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;

public class SliderTest extends BaseParallelApiTest {

    private final PrefsClient prefsClient = new PrefsClient();

    @Test(priority = 1)
    public void saveSlider_validValue_returns200() {
        Response r = prefsClient.saveSlider(token(), "test-slider", 75);
        r.then()
                .statusCode(200)
                .body("key", equalTo("test-slider"))
                .body("value", equalTo(75));
    }

    @Test(priority = 2)
    public void saveSlider_outOfRange_returns400() {
        Response r = prefsClient.saveSlider(token(), "test-slider-out", 150);
        r.then().statusCode(400);
    }
}
