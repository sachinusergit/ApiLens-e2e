package com.apilens.tests;

import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Proves database-per-service isolation:
 * the auth DB does NOT have the files table, and vice versa.
 */
public class IsolationTest extends BaseTest {

    private static final String AUTH_DB_URL = System.getenv().getOrDefault(
            "AUTH_DB_URL", "jdbc:postgresql://localhost:5433/apilens_auth");
    private static final String FILES_DB_URL = System.getenv().getOrDefault(
            "FILES_DB_URL", "jdbc:postgresql://localhost:5435/apilens_files");

    @Test
    public void authDb_doesNotHaveFilesTable() throws Exception {
        try (Connection c = DriverManager.getConnection(AUTH_DB_URL, "apilens", "apilens_dev");
             Statement st = c.createStatement()) {
            st.executeQuery("SELECT * FROM files");
            fail("auth DB should NOT have a files table — DBs are supposed to be isolated");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("does not exist"),
                    "Expected 'relation does not exist', got: " + e.getMessage());
        }
    }

    @Test
    public void filesDb_doesNotHaveUsersTable() throws Exception {
        try (Connection c = DriverManager.getConnection(FILES_DB_URL, "apilens", "apilens_dev");
             Statement st = c.createStatement()) {
            st.executeQuery("SELECT * FROM users");
            fail("files DB should NOT have a users table");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("does not exist"),
                    "Expected 'relation does not exist', got: " + e.getMessage());
        }
    }

    @Test
    public void authDb_hasUsersTable() throws Exception {
        try (Connection c = DriverManager.getConnection(AUTH_DB_URL, "apilens", "apilens_dev");
             Statement st = c.createStatement()) {
            st.executeQuery("SELECT COUNT(*) FROM users");
            assertTrue(true);
        }
    }

    @Test
    public void filesDb_hasFilesTable() throws Exception {
        try (Connection c = DriverManager.getConnection(FILES_DB_URL, "apilens", "apilens_dev");
             Statement st = c.createStatement()) {
            st.executeQuery("SELECT COUNT(*) FROM files");
            assertTrue(true);
        }
    }
}
