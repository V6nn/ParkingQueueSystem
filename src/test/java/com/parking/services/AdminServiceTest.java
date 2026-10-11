package com.parking.services;

import com.parking.database.DatabaseInitializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import static org.junit.Assert.*;

public class AdminServiceTest {

    private static final String SECURITY_QUESTION =
            "What was the name of your first school?";
    private static final String SECURITY_ANSWER = "North School";

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
            System.setProperty("parking.db.url", previousDatabaseUrl);
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
        createInitialAdmin();
        assertTrue(adminService.hasAdminAccount());
    }

    @Test
    public void loginSucceedsWithCorrectCredentials()
            throws SQLException {
        createInitialAdmin();
        assertTrue(adminService.authenticate(
                "admin", "SecurePass123"));
    }

    @Test
    public void loginFailsWithIncorrectPassword()
            throws SQLException {
        createInitialAdmin();
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
        createInitialAdmin();
        try {
            adminService.createInitialAdmin(
                    "anotherAdmin", "AnotherPass123",
                    SECURITY_QUESTION, SECURITY_ANSWER);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException exception) {
            // Expected result
        }
    }

    @Test
    public void passwordCanBeChanged() throws SQLException {
        createInitialAdmin();
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
        createInitialAdmin();
        assertFalse(adminService.changePassword(
                "admin", "WrongPassword123", "NewSecurePass456"));
        assertTrue(adminService.authenticate(
                "admin", "SecurePass123"));
    }

    @Test
    public void shortPasswordIsRejected() throws SQLException {
        try {
            adminService.createInitialAdmin(
                    "admin", "short",
                    SECURITY_QUESTION, SECURITY_ANSWER);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
            // Expected result
        }
    }

    @Test
    public void recoveryQuestionCanResetPasswordWithNormalizedAnswer()
            throws SQLException {
        createInitialAdmin();
        assertEquals(
                SECURITY_QUESTION,
                adminService.getSecurityQuestion("admin").orElseThrow());
        assertTrue(adminService.resetPassword(
                "admin", "  NORTH SCHOOL ", "RecoveredPass789"));
        assertTrue(adminService.authenticate(
                "admin", "RecoveredPass789"));
        assertFalse(adminService.authenticate(
                "admin", "SecurePass123"));
    }

    @Test
    public void recoveryRejectsIncorrectAnswer() throws SQLException {
        createInitialAdmin();
        assertFalse(adminService.resetPassword(
                "admin", "Wrong answer", "RecoveredPass789"));
        assertTrue(adminService.authenticate(
                "admin", "SecurePass123"));
    }

    @Test
    public void authenticatedAdminCanSetUpRecovery() throws SQLException {
        createInitialAdmin();

        assertTrue(adminService.configureRecovery(
                "admin", "SecurePass123",
                "What city were you born in?", "Halifax"));
        assertTrue(adminService.resetPassword(
                "admin", "halifax", "RecoveredPass789"));
        assertTrue(adminService.authenticate(
                "admin", "RecoveredPass789"));
    }

    @Test
    public void recoveryQuestionIsRequiredForNewAdmin() throws SQLException {
        try {
            adminService.createInitialAdmin(
                    "admin", "SecurePass123", " ", SECURITY_ANSWER);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().contains("security question"));
        }
    }

    private void createInitialAdmin() throws SQLException {
        assertTrue(adminService.createInitialAdmin(
                "admin", "SecurePass123",
                SECURITY_QUESTION, SECURITY_ANSWER));
    }
}
