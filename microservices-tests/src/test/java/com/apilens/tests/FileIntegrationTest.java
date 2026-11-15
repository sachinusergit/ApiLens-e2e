package com.apilens.tests;

import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.assertTrue;

public class FileIntegrationTest extends BaseTest {

    @Test(priority = 1)
    public void upload_writesRowToDatabase_thenDownloadReturnsBytes() throws Exception {
        String token = loginAsDemoUser();
        byte[] content = "hello apilens db test".getBytes(StandardCharsets.UTF_8);

        String fileId =
                given()
                        .header("Authorization", "Bearer " + token)
                        .multiPart("file", "dbtest.txt", content, "text/plain")
                        .when()
                        .post("/api/files/upload")
                        .then()
                        .statusCode(201)
                        .body("originalName", equalTo("dbtest.txt"))
                        .body("sizeBytes", equalTo(content.length))
                        .extract().path("id");

        // ✅ DB ASSERTION — proves the row was written to apilens_files
        String jdbcUrl = System.getenv().getOrDefault(
                "FILES_DB_URL", "jdbc:postgresql://localhost:5435/apilens_files");
        try (Connection c = DriverManager.getConnection(jdbcUrl, "apilens", "apilens_dev");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT original_name, size_bytes, status FROM files WHERE id = '" + fileId + "'")) {

            assertTrue(rs.next(), "Expected row in files table");
            assertTrue("dbtest.txt".equals(rs.getString("original_name")));
            assertTrue(rs.getLong("size_bytes") == content.length);
            assertTrue("AVAILABLE".equals(rs.getString("status")));
        }

        // ✅ DOWNLOAD round-trip
        byte[] downloaded =
                given()
                        .header("Authorization", "Bearer " + token)
                        .when()
                        .get("/api/files/" + fileId)
                        .then()
                        .statusCode(200)
                        .extract().asByteArray();
        assertTrue(java.util.Arrays.equals(content, downloaded));
    }

    @Test(priority = 2)
    public void download_unknownId_returnsError() {
        String token = loginAsDemoUser();
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/files/" + UUID.randomUUID())
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(404), equalTo(500)));
    }

    @Test(priority = 3)
    public void list_requiresAuth() {
        given()
                .when()
                .get("/api/files")
                .then()
                .statusCode(401);
    }

    @Test(priority = 5)
    public void search_byContentType_returnsMetaSummaryAndOnlyMatchingFiles() {
        String token = loginAsDemoUser();
        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("contentType", "text/plain")
                .when()
                .get("/api/files/search")
                .then()
                .statusCode(200)
                .body("meta.filterApplied", equalTo("contentType=text/plain"))
                .body("meta.totalCount", greaterThan(0))
                .body("meta.orgId", equalTo("00000000-0000-0000-0000-000000000001"))
                .body("meta.executionTimeMs", greaterThanOrEqualTo(0))
                .body("summary.totalFiles", greaterThan(0))
                .body("summary.totalSizeHuman", not(emptyString()))
                .body("summary.byCategory.document", greaterThan(0))
                .body("files", not(empty()))
                .body("files.contentType", everyItem(equalTo("text/plain")))
                .body("files.extension", everyItem(equalTo(".txt")))
                .body("files.sizeHuman", everyItem(not(emptyString())))
                .body("files.createdAtHuman", everyItem(not(emptyString())))
                .body("files.tags", everyItem(hasItem("active")));
    }

    @Test(priority = 6)
    public void search_deletedStatus_returnsSoftDeletedRows() {
        String token = loginAsDemoUser();
        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("status", "DELETED")
                .when()
                .get("/api/files/search")
                .then()
                .statusCode(200)
                .body("meta.filterApplied", equalTo("status=DELETED"))
                .body("meta.totalCount", greaterThan(0))
                .body("summary.byCategory.document", greaterThan(0))
                .body("files.status", everyItem(equalTo("DELETED")))
                .body("files.tags", everyItem(hasItem("deleted")));
    }

    @Test(priority = 7)
    public void markdownUpload_isDetectedAsTextMarkdown_andFilteredCorrectly() {
        String token = loginAsDemoUser();
        String name = "notes-" + UUID.randomUUID() + ".md";
        byte[] content = "# markdown".getBytes(StandardCharsets.UTF_8);

        given()
                .header("Authorization", "Bearer " + token)
                .multiPart("file", name, content, "application/octet-stream")
                .when()
                .post("/api/files/upload")
                .then()
                .statusCode(201)
                .body("contentType", equalTo("text/markdown"));

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("contentType", "text/markdown")
                .when()
                .get("/api/files/search")
                .then()
                .statusCode(200)
                .body("meta.filterApplied", equalTo("contentType=text/markdown"))
                .body("files.originalName", hasItem(name))
                .body("files.contentType", everyItem(equalTo("text/markdown")))
                .body("files.extension", everyItem(equalTo(".md")))
                .body("files.tags", everyItem(hasItem("md")));
    }

    @Test(priority = 8)
    public void search_postBody_returnsSameEnvelope() {
        String token = loginAsDemoUser();
        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"contentType\":\"text/plain\"}")
                .when()
                .post("/api/files/search")
                .then()
                .statusCode(200)
                .body("meta.filterApplied", equalTo("contentType=text/plain"))
                .body("files.contentType", everyItem(equalTo("text/plain")));
    }

    @Test(priority = 4)
    public void delete_softDeletes_rowStaysInDb() throws Exception {
        String token = loginAsDemoUser();
        byte[] content = "to be deleted".getBytes(StandardCharsets.UTF_8);

        String fileId =
                given()
                        .header("Authorization", "Bearer " + token)
                        .multiPart("file", "delete-me.txt", content, "text/plain")
                        .when()
                        .post("/api/files/upload")
                        .then()
                        .statusCode(201)
                        .extract().path("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/files/" + fileId)
                .then()
                .statusCode(204);

        // ✅ Row still exists, but deleted_at set + status DELETED
        String jdbcUrl = System.getenv().getOrDefault(
                "FILES_DB_URL", "jdbc:postgresql://localhost:5435/apilens_files");
        try (Connection c = DriverManager.getConnection(jdbcUrl, "apilens", "apilens_dev");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT status, deleted_at FROM files WHERE id = '" + fileId + "'")) {

            assertTrue(rs.next());
            assertTrue("DELETED".equals(rs.getString("status")));
            assertTrue(rs.getTimestamp("deleted_at") != null);
        }
    }
}
