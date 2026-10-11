package com.parking.services;

import com.parking.database.DatabaseInitializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import static org.junit.Assert.*;

public class AdminServiceTest {

    private AdminService adminService;
    private Path tempDatabase;
    private String previousDatabaseUrl;

    @Before
    public void setUp() throws Exception {
        previousDatabaseUrl = System.getProperty("parking.db.url");

        tempDatabase = Files.createTempFile(
                "parking-admin-test-", ".db");

        System.setProperty(
                "parking.db.url",
                "jdbc:sqlite:" + tempDatabase.toAbsolutePath());

        DatabaseInitializer.initialize();

        adminService = new AdminService();
    }

    @After
    public void tearDown() throws Exception {
        if (previousDatabaseUrl == null) {
            System.clearProperty("parking.db.url");
        } else {
            System.setProperty(
                    "parking.db.url", previousDatabaseUrl);
        }

        if (tempDatabase != null) {
            Files.deleteIfExists(tempDatabase);
            Files.deleteIfExists(
                    Path.of(tempDatabase.toString() + "-wal"));
            Files.deleteIfExists(
                    Path.of(tempDatabase.toString() + "-shm"));
        }
    }

    @Test
    public void noAdminExistsInitially() throws SQLException {
        assertFalse(adminService.hasAdminAccount());
    }

    @Test
    public void createInitialAdminSuccessfully() throws SQLException {
        assertTrue(adminService.createInitialAdmin(
                "admin", "SecurePass123"));

        assertTrue(adminService.hasAdminAccount());
    }

    @Test
    public void loginSucceedsWithCorrectCredentials()
            throws SQLException {
        adminService.createInitialAdmin(
                "admin", "SecurePass123");

        assertTrue(adminService.authenticate(
                "admin", "SecurePass123"));
    }

    @Test
    public void loginFailsWithIncorrectPassword()
            throws SQLException {
        adminService.createInitialAdmin(
                "admin", "SecurePass123");

        assertFalse(adminService.authenticate(
                "admin", "WrongPassword123"));
    }

    @Test
    public void loginFailsForUnknownUsername()
            throws SQLException {
        assertFalse(adminService.authenticate(
                "unknown", "SecurePass123"));
    }

    @Test
    public void secondInitialAdminIsRejected() throws SQLException {
        adminService.createInitialAdmin(
                "admin", "SecurePass123");

        try {
            adminService.createInitialAdmin(
                    "anotherAdmin", "AnotherPass123");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException exception) {
            // Expected result
        }
    }

    @Test
    public void passwordCanBeChanged() throws SQLException {
        adminService.createInitialAdmin(
                "admin", "SecurePass123");

        assertTrue(adminService.changePassword(
                "admin", "SecurePass123", "NewSecurePass456"));

        assertFalse(adminService.authenticate(
                "admin", "SecurePass123"));

        assertTrue(adminService.authenticate(
                "admin", "NewSecurePass456"));
    }

    @Test
    public void passwordChangeFailsWithIncorrectCurrentPassword()
            throws SQLException {
        adminService.createInitialAdmin(
                "admin", "SecurePass123");

        assertFalse(adminService.changePassword(
                "admin", "WrongPassword123", "NewSecurePass456"));

        assertTrue(adminService.authenticate(
                "admin", "SecurePass123"));
    }

    @Test
    public void shortPasswordIsRejected() throws SQLException {
        try {
            adminService.createInitialAdmin("admin", "short");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
            // Expected result
        }
    }
}