package com.parking.services;

import com.parking.database.DatabaseConnection;
import com.parking.database.DatabaseInitializer;
import com.parking.models.ParkingLot;
import com.parking.models.ParkingRecord;
import com.parking.models.VehicleTransaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.*;

public class VehicleTransactionServiceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private String previousDatabaseUrl;
    private final VehicleTransactionService transactionService =
            new VehicleTransactionService();
    private final ParkingLotService parkingLotService =
            new ParkingLotService();
    private final ParkingRecordService parkingRecordService =
            new ParkingRecordService();

    @Before
    public void setUp() throws Exception {
        previousDatabaseUrl = System.getProperty("parking.db.url");
        System.setProperty(
                "parking.db.url",
                "jdbc:sqlite:"
                        + temporaryFolder.newFile("vehicle-transactions.db")
                                .getAbsolutePath());

        DatabaseInitializer.initialize();
        parkingLotService.save(
                new ParkingLot(0, "Transaction Test Lot", 2,
                        "08:00", "22:00"));
        parkingRecordService.save(
                new ParkingRecord(0, "2000-01-01 10:00:00", 0, 2), 2);
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
    public void entryAndPayAtExitKeepStatusAndOccupancyInSync()
            throws SQLException {
        VehicleTransaction entered =
                transactionService.recordVehicleEntry("ABC123", "CAR");

        assertEquals("OCCUPIED", entered.getParkingStatus());
        assertEquals(1, parkingRecordService.findLatest().getOccupiedSpaces());
        assertEquals(1, parkingRecordService.findLatest().getAvailableSpaces());

        VehicleTransaction exited =
                transactionService.payAtExit("ABC123");

        assertEquals("PAID", exited.getPaymentStatus());
        assertEquals("AVAILABLE", exited.getParkingStatus());
        assertNotNull(exited.getExitedAt());
        assertEquals(0, parkingRecordService.findLatest().getOccupiedSpaces());
        assertEquals(2, parkingRecordService.findLatest().getAvailableSpaces());

        List<VehicleTransaction> transactions =
                transactionService.findAll();
        assertEquals(1, transactions.size());
        assertEquals("PAID", transactions.get(0).getPaymentStatus());
        assertEquals("AVAILABLE", transactions.get(0).getParkingStatus());
        assertEquals(exited.getExitedAt(), transactions.get(0).getExitedAt());
    }

    @Test(expected = IllegalArgumentException.class)
    public void entryCannotExceedParkingCapacity() throws SQLException {
        transactionService.recordVehicleEntry("ABC123", "CAR");
        transactionService.recordVehicleEntry("XYZ789", "CAR");
        transactionService.recordVehicleEntry("LMN456", "CAR");
    }

    @Test
    public void entryRejectsOccupancyOutsideConfiguredCapacity()
            throws Exception {
        ParkingRecord current = parkingRecordService.findLatest();
        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE parking_records
                     SET occupied_spaces = 3, available_spaces = 0
                     WHERE id = ?
                     """)) {
            statement.setInt(1, current.getId());
            assertEquals(1, statement.executeUpdate());
        }

        try {
            transactionService.recordVehicleEntry("ABC123", "CAR");
            fail("Expected inconsistent occupancy to be rejected.");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("outside"));
        }

        assertTrue(transactionService.findAll().isEmpty());
    }
}
