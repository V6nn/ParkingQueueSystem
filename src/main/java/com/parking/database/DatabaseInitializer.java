package com.parking.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    private DatabaseInitializer() {
        // Prevent creating an instance of this class
    }

    public static void initialize() {
        try (Connection connection = DatabaseConnection.connect();
             Statement statement = connection.createStatement()) {

            createParkingLotTable(statement);
            createEntrancesTable(statement);
            createExitsTable(statement);
            createParkingRecordsTable(statement);
            createEntranceRecordsTable(statement);
            createExitRecordsTable(statement);
            createCongestionRulesTable(statement);
            createRecommendationsTable(statement);

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Database initialization failed.");
            e.printStackTrace();
        }
    }

    private static void createParkingLotTable(Statement statement)
            throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS parking_lot (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    total_spaces INTEGER NOT NULL CHECK (total_spaces > 0),
                    operating_start TEXT NOT NULL,
                    operating_end TEXT NOT NULL
                )
                """;

        statement.execute(sql);
    }

    private static void createEntrancesTable(Statement statement)
            throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS entrances (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    lane_count INTEGER NOT NULL CHECK (lane_count > 0),
                    active_lanes INTEGER NOT NULL CHECK (
                        active_lanes >= 0
                        AND active_lanes <= lane_count
                    ),
                    status TEXT NOT NULL CHECK (
                        status IN ('OPEN', 'CLOSED')
                    )
                )
                """;

        statement.execute(sql);
    }

    private static void createExitsTable(Statement statement)
            throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS exits (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    lane_count INTEGER NOT NULL CHECK (lane_count > 0),
                    active_lanes INTEGER NOT NULL CHECK (
                        active_lanes >= 0
                        AND active_lanes <= lane_count
                    ),
                    status TEXT NOT NULL CHECK (
                        status IN ('OPEN', 'CLOSED')
                    )
                )
                """;

        statement.execute(sql);
    }

    private static void createParkingRecordsTable(Statement statement)
            throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS parking_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    recorded_at TEXT NOT NULL,
                    occupied_spaces INTEGER NOT NULL CHECK (occupied_spaces >= 0),
                    available_spaces INTEGER NOT NULL CHECK (available_spaces >= 0)
                )
                """;

        statement.execute(sql);
    }

    private static void createEntranceRecordsTable(Statement statement)
            throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS entrance_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    entrance_id INTEGER NOT NULL,
                    recorded_at TEXT NOT NULL,
                    queue_length INTEGER NOT NULL CHECK (queue_length >= 0),
                    vehicles_entered INTEGER NOT NULL CHECK (vehicles_entered >= 0),
                    average_service_time REAL NOT NULL CHECK (
                        average_service_time >= 0
                    ),
                    FOREIGN KEY (entrance_id)
                        REFERENCES entrances(id)
                        ON UPDATE CASCADE
                        ON DELETE RESTRICT
                )
                """;

        statement.execute(sql);
    }

    private static void createExitRecordsTable(Statement statement)
            throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS exit_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    exit_id INTEGER NOT NULL,
                    recorded_at TEXT NOT NULL,
                    vehicles_exited INTEGER NOT NULL CHECK (vehicles_exited >= 0),
                    FOREIGN KEY (exit_id)
                        REFERENCES exits(id)
                        ON UPDATE CASCADE
                        ON DELETE RESTRICT
                )
                """;

        statement.execute(sql);
    }

    private static void createCongestionRulesTable(Statement statement)
            throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS congestion_rules (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    rule_name TEXT NOT NULL UNIQUE,
                    threshold REAL NOT NULL,
                    unit TEXT NOT NULL,
                    description TEXT,
                    enabled INTEGER NOT NULL DEFAULT 1 CHECK (
                        enabled IN (0, 1)
                    )
                )
                """;

        statement.execute(sql);
    }

    private static void createRecommendationsTable(Statement statement)
            throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS recommendations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    entrance_id INTEGER NOT NULL,
                    created_at TEXT NOT NULL,
                    current_queue INTEGER NOT NULL CHECK (current_queue >= 0),
                    recommended_lanes INTEGER NOT NULL CHECK (recommended_lanes >= 0),
                    recommended_capacity INTEGER NOT NULL CHECK (
                        recommended_capacity >= 0
                    ),
                    reason TEXT,
                    FOREIGN KEY (entrance_id)
                        REFERENCES entrances(id)
                        ON UPDATE CASCADE
                        ON DELETE RESTRICT
                )
                """;

        statement.execute(sql);
    }
}