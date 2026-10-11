package com.parking.database;

import com.parking.optimization.OptimizationService;
import com.parking.models.EntranceRecord;
import com.parking.models.ParkingLot;
import com.parking.models.ParkingRecord;
import com.parking.services.EntranceRecordService;
import com.parking.services.ParkingLotService;
import com.parking.services.ParkingRecordService;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

import static org.junit.Assert.*;

public class DatabaseInitializerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private String previousDatabaseUrl;
    private String testDatabaseUrl;

    @Before
    public void createLegacyDatabase() throws Exception {
        previousDatabaseUrl = System.getProperty("parking.db.url");
        testDatabaseUrl = "jdbc:sqlite:"
                + temporaryFolder.newFile("legacy-parking.db")
                        .getAbsolutePath();
        System.setProperty("parking.db.url", testDatabaseUrl);

        try (Connection connection =
                     DriverManager.getConnection(testDatabaseUrl);
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE entrances (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE,
                        lane_count INTEGER NOT NULL,
                        active_lanes INTEGER NOT NULL,
                        status TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE entrance_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        entrance_id INTEGER NOT NULL,
                        recorded_at TEXT NOT NULL,
                        queue_length INTEGER NOT NULL,
                        vehicles_entered INTEGER NOT NULL,
                        average_service_time REAL NOT NULL,
                        FOREIGN KEY (entrance_id) REFERENCES entrances(id)
                    )
                    """);
            statement.execute("""
                    INSERT INTO entrances (
                        id, name, lane_count, active_lanes, status
                    )
                    VALUES (1, 'Legacy entrance', 2, 1, 'OPEN')
                    """);
            statement.execute("""
                    INSERT INTO entrance_records (
                        id, entrance_id, recorded_at, queue_length,
                        vehicles_entered, average_service_time
                    )
                    VALUES (
                        10, 1, '2026-10-11 10:00:00', 7, 5, 30
                    )
                    """);
        }
    }

    @After
    public void restoreDatabaseUrl() {
        if (previousDatabaseUrl == null) {
            System.clearProperty("parking.db.url");
        } else {
            System.setProperty("parking.db.url", previousDatabaseUrl);
        }
    }

    @Test
    public void migrationPreservesQueueRecordsAndLeavesNewDurationMissing()
            throws Exception {
        DatabaseInitializer.initialize();

        Optional<EntranceRecord> migrated =
                new EntranceRecordService().findLatestByEntranceId(1);

        assertTrue(migrated.isPresent());
        assertEquals(10, migrated.get().getId());
        assertEquals(7, migrated.get().getQueueLength());
        assertEquals(5, migrated.get().getVehiclesEntered());
        assertEquals(30.0, migrated.get().getAverageServiceTime(), 0.001);
        assertNull(migrated.get().getObservationDurationMinutes());
    }

    @Test
    public void migrationAddsAdminRecoveryFieldsWithoutRemovingAccount()
            throws Exception {
        try (Connection connection =
                     DriverManager.getConnection(testDatabaseUrl);
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE admins (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL UNIQUE,
                        password_hash TEXT NOT NULL,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.execute("""
                    INSERT INTO admins (username, password_hash)
                    VALUES ('legacy-admin', 'existing-hash')
                    """);
        }

        DatabaseInitializer.initialize();

        try (Connection connection =
                     DriverManager.getConnection(testDatabaseUrl);
             Statement statement = connection.createStatement();
             var columns = statement.executeQuery(
                     "PRAGMA table_info(admins)")) {
            java.util.Set<String> names = new java.util.HashSet<>();
            while (columns.next()) {
                names.add(columns.getString("name"));
            }
            assertTrue(names.contains("security_question"));
            assertTrue(names.contains("security_answer_hash"));
        }
        try (Connection connection =
                     DriverManager.getConnection(testDatabaseUrl);
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT username, password_hash
                     FROM admins
                     WHERE id = 1
                     """);
             var account = statement.executeQuery()) {
            assertTrue(account.next());
            assertEquals("legacy-admin", account.getString("username"));
            assertEquals("existing-hash", account.getString("password_hash"));
        }
    }

    @Test
    public void savedDataDrivesRecommendationsAndHonorsFullCapacity()
            throws Exception {
        DatabaseInitializer.initialize();

        ParkingLotService parkingLotService = new ParkingLotService();
        parkingLotService.save(
                new ParkingLot(0, "Recommendation Test Lot", 20,
                        "08:00", "22:00"));

        ParkingRecordService parkingRecordService =
                new ParkingRecordService();
        parkingRecordService.save(
                new ParkingRecord(
                        0, "2026-10-12 10:00:00", 3, 17), 20);

        try (Connection connection =
                     DriverManager.getConnection(testDatabaseUrl);
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO parking_records (
                         recorded_at, occupied_spaces, available_spaces
                     )
                     VALUES (?, ?, ?)
                     """)) {
            statement.setString(1, "2026-10-12T09:59:00.123456");
            statement.setInt(2, 45);
            statement.setInt(3, 0);
            statement.executeUpdate();
        }

        assertEquals(3,
                parkingRecordService.findLatest().getOccupiedSpaces());

        OptimizationService optimizationService =
                new OptimizationService();
        OptimizationService.OptimizationRecommendation historical =
                optimizationService.generateRecommendation(1, 30, 4)
                        .orElseThrow();
        assertFalse(historical.getPredictedQueueLength().isPresent());

        new EntranceRecordService().save(
                new EntranceRecord(
                        0, 1, "2026-10-12 11:00:00", 8, 15,
                        30.0, 2.5));

        OptimizationService.OptimizationRecommendation current =
                optimizationService.generateRecommendation(1, 30, 4)
                        .orElseThrow();
        assertEquals(2, current.getRecommendedLanes().getAsInt());
        assertEquals(2.0,
                current.getEstimatedWaitMinutes().getAsDouble(), 0.001);
        assertEquals(18, current.getPredictedQueueLength().getAsInt());
        assertEquals(17, current.getRecommendedCapacity().getAsInt());

        parkingRecordService.save(
                new ParkingRecord(
                        0, "2026-10-12 12:00:00", 20, 0), 20);
        OptimizationService.OptimizationRecommendation full =
                optimizationService.generateRecommendation(1, 30, 4)
                        .orElseThrow();
        assertEquals(0, full.getRecommendedCapacity().getAsInt());
    }
}
