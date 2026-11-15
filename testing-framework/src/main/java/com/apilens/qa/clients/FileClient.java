package com.apilens.qa.clients;

import com.apilens.qa.config.TestConfig;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * File endpoints (via gateway; it injects X-User-Id/X-Org-Id from the JWT):
 *   GET    /api/files                          -> 200 list
 *   POST   /api/files/upload                   -> 201 (part name must be "file")
 *   GET    /api/files/{id}                     -> 200 stream | 400 (not found / wrong org)
 *   DELETE /api/files/{id}                     -> 204 (soft delete)
 *   GET    /api/files/search?contentType=...   -> 200 {meta, summary, files}
 *   GET    /api/files/autocomplete?q=..        -> 200 list
 */
public class FileClient {

    public Response list(String token) {
        return given()
                .baseUri(TestConfig.BASE_URL)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/files");
    }

    public Response upload(String token, String fileName, byte[] content, String contentType) {
        return given()
                .baseUri(TestConfig.BASE_URL)
                .header("Authorization", "Bearer " + token)
                .multiPart("file", fileName, content, contentType)
                .when()
                .post("/api/files/upload");
    }

    public Response download(String token, String fileId) {
        return given()
                .baseUri(TestConfig.BASE_URL)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/files/" + fileId);
    }

    public Response delete(String token, String fileId) {
        return given()
                .baseUri(TestConfig.BASE_URL)
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/files/" + fileId);
    }

    public Response search(String token, String contentType) {
        return given()
                .baseUri(TestConfig.BASE_URL)
                .header("Authorization", "Bearer " + token)
                .queryParam("contentType", contentType)
                .when()
                .get("/api/files/search");
    }

    public Response autocomplete(String token, String query) {
        return given()
                .baseUri(TestConfig.BASE_URL)
                .header("Authorization", "Bearer " + token)
                .queryParam("q", query)
                .when()
                .get("/api/files/autocomplete");
    }
}
