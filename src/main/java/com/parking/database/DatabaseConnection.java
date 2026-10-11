package com.parking.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String DEFAULT_URL = "jdbc:sqlite:parking.db";

    private DatabaseConnection() {
        // Prevent creating an instance of this class
    }

    public static Connection connect() throws SQLException {
        String url = System.getProperty("parking.db.url", DEFAULT_URL);

        Connection connection = DriverManager.getConnection(url);

        // Enable foreign key support in SQLite
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }

        return connection;
    }
}