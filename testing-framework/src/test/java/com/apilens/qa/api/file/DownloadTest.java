package com.apilens.qa.api.file;

import com.apilens.qa.api.BaseParallelApiTest;
import com.apilens.qa.clients.FileClient;
import com.apilens.qa.fixtures.FileFactory;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.assertTrue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.anyOf;

public class DownloadTest extends BaseParallelApiTest {

    private final FileClient fileClient = new FileClient();

    @Test(priority = 1)
    public void download_uploadedFile_returnsSameBytes() {
        String fileName = FileFactory.uniqueFileName("txt");
        byte[] content = FileFactory.textContent();

        String fileId = fileClient.upload(token(), fileName, content, "text/plain")
                .then().statusCode(201).extract().path("id");

        byte[] downloaded = fileClient.download(token(), fileId)
                .then().statusCode(200).extract().asByteArray();

        assertTrue(Arrays.equals(content, downloaded));
    }

    @Test(priority = 2)
    public void download_unknownFile_returns400() {
        Response r = fileClient.download(token(), "00000000-0000-0000-0000-000000000000");
        r.then().statusCode(anyOf(equalTo(400), equalTo(404)));
    }
}
