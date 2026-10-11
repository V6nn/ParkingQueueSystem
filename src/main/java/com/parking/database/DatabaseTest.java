
package com.parking.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseTest {

    public static void main(String[] args) {

        DatabaseInitializer.initialize();

        try (Connection connection = DatabaseConnection.connect();
             Statement statement = connection.createStatement()) {

            String sql = """
                    SELECT name
                    FROM sqlite_master
                    WHERE type = 'table'
                    ORDER BY name
                    """;

            try (ResultSet resultSet = statement.executeQuery(sql)) {
                System.out.println();
                System.out.println("Tables in parking.db:");

                while (resultSet.next()) {
                    System.out.println("- " + resultSet.getString("name"));
                }
            }

            try (ResultSet resultSet =
                    statement.executeQuery("PRAGMA foreign_keys")) {

                if (!resultSet.next() || resultSet.getInt(1) != 1) {
                    throw new SQLException(
                            "Foreign key enforcement is disabled.");
                }

                System.out.println();
                System.out.println("Foreign key enforcement: ENABLED");
            }

            testInvalidEntranceReference(connection, statement);

        } catch (SQLException e) {
            System.err.println("Database test failed.");
            e.printStackTrace();
        }
    }

    private static void testInvalidEntranceReference(
            Connection connection, Statement statement)
            throws SQLException {

        connection.setAutoCommit(false);
        boolean rejected = false;

        try {
            statement.executeUpdate("""
                    INSERT INTO entrance_records (
                        entrance_id,
                        recorded_at,
                        queue_length,
                        vehicles_entered,
                        average_service_time
                    )
                    VALUES (
                        -1,
                        '2099-01-01 10:00:00',
                        0,
                        0,
                        0.0
                    )
                    """);

        } catch (SQLException e) {
            String message = e.getMessage();

            if (message != null
                    && message.toLowerCase().contains(
                            "foreign key constraint failed")) {
                rejected = true;
            } else {
                throw e;
            }

        } finally {
            try {
                connection.rollback();
            } finally {
                connection.setAutoCommit(true);
            }
        }

        if (!rejected) {
            throw new SQLException(
                    "Foreign key test failed: invalid entrance ID was accepted.");
        }

        System.out.println(
                "Foreign key rejection test: PASSED");
    }
}