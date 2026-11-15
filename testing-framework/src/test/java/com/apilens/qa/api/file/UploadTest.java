package com.apilens.qa.api.file;

import com.apilens.qa.api.BaseParallelApiTest;
import com.apilens.qa.clients.FileClient;
import com.apilens.qa.fixtures.FileFactory;
import com.apilens.qa.config.TestConfig;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class UploadTest extends BaseParallelApiTest {

    private final FileClient fileClient = new FileClient();

    @Test(priority = 1)
    public void upload_textFile_returns201() {
        String fileName = FileFactory.uniqueFileName("txt");
        byte[] content = FileFactory.textContent();

        Response r = fileClient.upload(token(), fileName, content, "text/plain");
        r.then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("originalName", equalTo(fileName))
                .body("sizeBytes", equalTo(content.length))
                .body("status", equalTo("AVAILABLE"));
    }

    @Test(priority = 2)
    public void upload_withoutToken_returns401() {
        Response r = given()
                .baseUri(TestConfig.BASE_URL)
                .multiPart("file", "test.txt", "content".getBytes(), "text/plain")
                .when()
                .post("/api/files/upload");
        r.then().statusCode(401);
    }

    @Test(priority = 3)
    public void upload_multipleFiles_independent() {
        for (int i = 0; i < 3; i++) {
            String fileName = FileFactory.uniqueFileName("txt");
            Response r = fileClient.upload(token(), fileName, FileFactory.textContent(), "text/plain");
            r.then().statusCode(201);
        }
    }
}
