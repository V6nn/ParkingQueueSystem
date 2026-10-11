package com.parking.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    private DatabaseInitializer() {
        // Prevent creating an instance of this class
    }

    private static void createSingleParkingLotIndex(Statement statement)
            throws SQLException {

        String sql = """
                CREATE UNIQUE INDEX IF NOT EXISTS
                one_parking_lot_only
                ON parking_lot ((1))
                """;

        statement.execute(sql);
    }

    private static void createAdminsTable(Statement statement)
            throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS admins (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    security_question TEXT,
                    security_answer_hash TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;

        statement.execute(sql);
    }

    private static void ensureAdminRecoveryColumns(Statement statement)
            throws SQLException {
        boolean hasQuestion = false;
        boolean hasAnswerHash = false;

        try (ResultSet columns =
                     statement.executeQuery("PRAGMA table_info(admins)")) {
            while (columns.next()) {
                String name = columns.getString("name");
                hasQuestion |= "security_question".equals(name);
                hasAnswerHash |= "security_answer_hash".equals(name);
            }
        }

        if (!hasQuestion) {
            statement.execute("""
                    ALTER TABLE admins
                    ADD COLUMN security_question TEXT
                    """);
        }

        if (!hasAnswerHash) {
            statement.execute("""
                    ALTER TABLE admins
                    ADD COLUMN security_answer_hash TEXT
                    """);
        }
    }

    private static void createSingleAdminIndex(Statement statement)
            throws SQLException {
        statement.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS one_admin_account
                ON admins ((1))
                """);
    }

    public static void initialize() {
        try (Connection connection = DatabaseConnection.connect();
             Statement statement = connection.createStatement()) {

            createParkingLotTable(statement);
            createSingleParkingLotIndex(statement);
            createAdminsTable(statement);
            ensureAdminRecoveryColumns(statement);
            createSingleAdminIndex(statement);
            createEntrancesTable(statement);
            createExitsTable(statement);
            createParkingRecordsTable(statement);
            createEntranceRecordsTable(statement);
            ensureEntranceObservationDurationColumn(statement);
            createExitRecordsTable(statement);
            createCongestionRulesTable(statement);
            createRecommendationsTable(statement);
            createVehicleTransactionsTable(statement);

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Database initialization failed.", e);
        }
    }

    private static void createVehicleTransactionsTable(
            Statement statement) throws SQLException {

        boolean tableExists;

        try (ResultSet result = statement.executeQuery("""
                SELECT sql
                FROM sqlite_master
                WHERE type = 'table'
                AND name = 'vehicle_transactions'
                """)) {

            tableExists = result.next();

            if (tableExists) {
                String tableSql = result.getString("sql");

                // The table is already migrated.
                if (tableSql.contains("'PROCESSING'")
                        && tableSql.contains("'OCCUPIED'")) {

                    createActiveVehicleIndex(statement);
                    return;
                }
            }
        }

        if (tableExists) {
            // Remove the old index before renaming the legacy table.
            statement.execute("""
                    DROP INDEX IF EXISTS unique_active_vehicle_plate
                    """);

            statement.execute("""
                    ALTER TABLE vehicle_transactions
                    RENAME TO vehicle_transactions_legacy
                    """);
        }

        statement.execute("""
                CREATE TABLE vehicle_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    plate_number TEXT NOT NULL,
                    vehicle_type TEXT NOT NULL
                        CHECK (vehicle_type IN ('CAR', 'MOTORCYCLE')),
                    amount REAL NOT NULL CHECK (amount >= 0),
                    payment_status TEXT NOT NULL
                        CHECK (payment_status IN (
                            'PAID', 'PROCESSING', 'FAILED'
                        )),
                    parking_status TEXT NOT NULL
                        CHECK (parking_status IN (
                            'RESERVED', 'OCCUPIED', 'AVAILABLE'
                        )),
                    created_at TEXT NOT NULL,
                    scheduled_arrival TEXT,
                    entered_at TEXT,
                    exited_at TEXT
                )
                """);

        if (tableExists) {
            // Preserve old records and map the previous statuses.
            statement.execute("""
                    INSERT INTO vehicle_transactions (
                        id, plate_number, vehicle_type, amount,
                        payment_status, parking_status, created_at
                    )
                    SELECT
                        id,
                        plate_number,
                        vehicle_type,
                        amount,
                        payment_status,
                        CASE parking_status
                            WHEN 'PARKED' THEN 'OCCUPIED'
                            WHEN 'EXITED' THEN 'AVAILABLE'
                            ELSE parking_status
                        END,
                        created_at
                    FROM vehicle_transactions_legacy
                    """);

            statement.execute("""
                    DROP TABLE vehicle_transactions_legacy
                    """);
        }

        createActiveVehicleIndex(statement);
    }

    private static void createActiveVehicleIndex(Statement statement)
            throws SQLException {

        statement.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS
                unique_active_vehicle_plate
                ON vehicle_transactions (plate_number)
                WHERE parking_status IN ('RESERVED', 'OCCUPIED')
                """);
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
                    observation_duration_minutes REAL CHECK (
                        observation_duration_minutes IS NULL
                        OR observation_duration_minutes > 0
                    ),
                    FOREIGN KEY (entrance_id)
                        REFERENCES entrances(id)
                        ON UPDATE CASCADE
                        ON DELETE RESTRICT
                )
                """;

        statement.execute(sql);
    }

    private static void ensureEntranceObservationDurationColumn(
            Statement statement) throws SQLException {

        try (ResultSet columns =
                     statement.executeQuery("PRAGMA table_info(entrance_records)")) {
            while (columns.next()) {
                if ("observation_duration_minutes"
                        .equals(columns.getString("name"))) {
                    return;
                }
            }
        }

        statement.execute("""
                ALTER TABLE entrance_records
                ADD COLUMN observation_duration_minutes REAL CHECK (
                    observation_duration_minutes IS NULL
                    OR observation_duration_minutes > 0
                )
                """);
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