
package com.parking.database;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String DEFAULT_URL =
            "jdbc:sqlite:"
            + Path.of("parking.db").toAbsolutePath().normalize();

    private DatabaseConnection() {
        // Prevent creating an instance
    }

    public static Connection connect() throws SQLException {
        String url = System.getProperty(
                "parking.db.url", DEFAULT_URL);
System.out.println("DATABASE URL: " + url);

        Connection connection = DriverManager.getConnection(url);
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }

        return connection;
    }
}