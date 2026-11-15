package com.apilens.qa.api.file;

import com.apilens.qa.api.BaseParallelApiTest;
import com.apilens.qa.clients.FileClient;
import com.apilens.qa.fixtures.FileFactory;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;

public class SearchTest extends BaseParallelApiTest {

    private final FileClient fileClient = new FileClient();

    @Test(priority = 1)
    public void search_byContentType_returnsFiltered() {
        fileClient.upload(token(), FileFactory.uniqueFileName("txt"),
                FileFactory.textContent(), "text/plain").then().statusCode(201);

        Response r = fileClient.search(token(), "text/plain");
        r.then()
                .statusCode(200)
                .body("meta.totalCount", greaterThanOrEqualTo(1))
                .body("files", notNullValue());
    }

    @Test(priority = 2)
    public void autocomplete_returns200AndList() {
        Response r = fileClient.autocomplete(token(), "RE");
        r.then()
                .statusCode(200)
                .body("$", instanceOf(List.class));
    }
}
