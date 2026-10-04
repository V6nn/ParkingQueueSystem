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

        } catch (SQLException e) {
            System.err.println("Database test failed.");
            e.printStackTrace();
        }
    }
}