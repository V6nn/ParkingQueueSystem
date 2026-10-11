package com.parking.services;

import com.parking.database.DatabaseConnection;
import com.parking.security.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

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
            String username,
            String password,
            String securityQuestion,
            String securityAnswer) throws SQLException {

        validateCredentials(username, password, securityQuestion,
                securityAnswer);

        if (hasAdminAccount()) {
            throw new IllegalStateException(
                    "An admin account already exists.");
        }

        String sql = """
                INSERT INTO admins (
                    username, password_hash,
                    security_question, security_answer_hash
                )
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username.trim());
            statement.setString(
                    2, PasswordHasher.hashPassword(password));
            statement.setString(3, securityQuestion.trim());
            statement.setString(
                    4, PasswordHasher.hashPassword(
                            normalizeAnswer(securityAnswer)));

            statement.executeUpdate();
            return true;

        } catch (SQLException exception) {
            if (exception.getErrorCode() == 19) {
                if (hasAdminAccount()) {
                    throw new IllegalStateException(
                            "An admin account already exists.",
                            exception);
                }
                throw new IllegalArgumentException(
                        "That username is already in use.", exception);
            }

            throw exception;
        }
    }

    public Optional<String> getSecurityQuestion(String username)
            throws SQLException {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        String sql = """
                SELECT security_question, security_answer_hash
                FROM admins
                WHERE username = ?
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {
            statement.setString(1, username.trim());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                String question = result.getString("security_question");
                String answerHash =
                        result.getString("security_answer_hash");
                return question == null || question.isBlank()
                        || answerHash == null || answerHash.isBlank()
                        ? Optional.empty()
                        : Optional.of(question);
            }
        }
    }

    public boolean resetPassword(
            String username, String securityAnswer, String newPassword)
            throws SQLException {
        validatePassword(newPassword);
        if (username == null || username.isBlank()
                || securityAnswer == null
                || securityAnswer.isBlank()) {
            return false;
        }

        String sql = """
                SELECT security_answer_hash
                FROM admins
                WHERE username = ?
                """;
        String answerHash;
        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {
            statement.setString(1, username.trim());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return false;
                }
                answerHash = result.getString("security_answer_hash");
            }
        }
        if (answerHash == null
                || !PasswordHasher.verifyPassword(
                        normalizeAnswer(securityAnswer), answerHash)) {
            return false;
        }

        String updateSql = """
                UPDATE admins
                SET password_hash = ?
                WHERE username = ? AND security_answer_hash = ?
                """;
        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(updateSql)) {
            statement.setString(
                    1, PasswordHasher.hashPassword(newPassword));
            statement.setString(2, username.trim());
            statement.setString(3, answerHash);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean configureRecovery(
            String username,
            String currentPassword,
            String securityQuestion,
            String securityAnswer) throws SQLException {
        validateRecovery(securityQuestion, securityAnswer);
        if (!authenticate(username, currentPassword)) {
            return false;
        }

        String sql = """
                UPDATE admins
                SET security_question = ?, security_answer_hash = ?
                WHERE username = ?
                """;
        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {
            statement.setString(1, securityQuestion.trim());
            statement.setString(
                    2, PasswordHasher.hashPassword(
                            normalizeAnswer(securityAnswer)));
            statement.setString(3, username.trim());
            return statement.executeUpdate() == 1;
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
            String username,
            String password,
            String securityQuestion,
            String securityAnswer) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username is required.");
        }

        if (username.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "Username must be 100 characters or fewer.");
        }

        validatePassword(password);
        validateRecovery(securityQuestion, securityAnswer);
    }

    private void validateRecovery(
            String securityQuestion, String securityAnswer) {
        if (securityQuestion == null || securityQuestion.isBlank()
                || securityQuestion.trim().length() > 200) {
            throw new IllegalArgumentException(
                    "Choose a security question.");
        }
        if (securityAnswer == null
                || securityAnswer.trim().length() < 3
                || securityAnswer.length() > 128) {
            throw new IllegalArgumentException(
                    "Security answer must be between 3 and 128 characters.");
        }
    }

    private String normalizeAnswer(String answer) {
        return answer.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()
                || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters.");
        }

        if (password.length() > 128) {
            throw new IllegalArgumentException(
                    "Password must be 128 characters or fewer.");
        }
    }
}