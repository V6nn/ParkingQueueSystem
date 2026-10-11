package com.parking.services;

import com.parking.database.DatabaseConnection;
import com.parking.security.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminService {

    public boolean hasAdminAccount() throws SQLException {
        String sql = "SELECT 1 FROM admins LIMIT 1";

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            return result.next();
        }
    }

    public boolean createInitialAdmin(
            String username, String password) throws SQLException {

        validateCredentials(username, password);

        if (hasAdminAccount()) {
            throw new IllegalStateException(
                    "An admin account already exists.");
        }

        String sql = """
                INSERT INTO admins (username, password_hash)
                VALUES (?, ?)
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username.trim());
            statement.setString(
                    2, PasswordHasher.hashPassword(password));

            statement.executeUpdate();
            return true;

        } catch (SQLException exception) {
            if (exception.getErrorCode() == 19) {
                throw new IllegalArgumentException(
                        "That username is already in use.", exception);
            }

            throw exception;
        }
    }

    public boolean authenticate(
            String username, String password) throws SQLException {

        if (username == null || username.isBlank()
                || password == null || password.isEmpty()) {
            return false;
        }

        String sql = """
                SELECT password_hash
                FROM admins
                WHERE username = ?
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username.trim());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return false;
                }

                return PasswordHasher.verifyPassword(
                        password, result.getString("password_hash"));
            }
        }
    }

    public boolean changePassword(
            String username, String currentPassword,
            String newPassword) throws SQLException {

        if (!authenticate(username, currentPassword)) {
            return false;
        }

        validatePassword(newPassword);

        String sql = """
                UPDATE admins
                SET password_hash = ?
                WHERE username = ?
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1, PasswordHasher.hashPassword(newPassword));
            statement.setString(2, username.trim());

            return statement.executeUpdate() == 1;
        }
    }

    private void validateCredentials(
            String username, String password) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username is required.");
        }

        if (username.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "Username must be 100 characters or fewer.");
        }

        validatePassword(password);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters.");
        }

        if (password.length() > 128) {
            throw new IllegalArgumentException(
                    "Password must be 128 characters or fewer.");
        }
    }
}