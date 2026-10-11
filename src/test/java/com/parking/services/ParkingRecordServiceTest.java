
package com.parking.services;

import com.parking.database.DatabaseConnection;
import com.parking.database.DatabaseInitializer;
import com.parking.models.ParkingRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.*;

public class ParkingRecordServiceTest {

    private ParkingRecordService service;
    private int firstTestId;
    private int secondTestId;

    @Before
    public void setUp() {
        DatabaseInitializer.initialize();
        service = new ParkingRecordService();
        firstTestId = 0;
        secondTestId = 0;
    }

    @After
    public void cleanUp() throws SQLException {
        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM parking_records WHERE id = ?")) {

            if (firstTestId > 0) {
                statement.setInt(1, firstTestId);
                statement.executeUpdate();
            }

            if (secondTestId > 0) {
                statement.setInt(1, secondTestId);
                statement.executeUpdate();
            }
        }
    }

    @Test
    public void saveAndFindLatestRecord() throws SQLException {
        ParkingRecord record = new ParkingRecord(
                0, "2099-01-01 10:00:00", 60, 40);

        service.save(record, 100);
        firstTestId = record.getId();

        assertTrue(firstTestId > 0);

        ParkingRecord latest = service.findLatest();

        assertNotNull(latest);
        assertEquals(firstTestId, latest.getId());
        assertEquals(60, latest.getOccupiedSpaces());
        assertEquals(40, latest.getAvailableSpaces());
    }

    @Test
    public void findAllReturnsSavedRecord() throws SQLException {
        ParkingRecord record = new ParkingRecord(
                0, "2099-01-02 10:00:00", 70, 30);

        service.save(record, 100);
        firstTestId = record.getId();

        List<ParkingRecord> records = service.findAll();

        assertTrue(records.stream().anyMatch(
                item -> item.getId() == firstTestId));
    }

    @Test
    public void rejectOccupiedSpacesAboveCapacity() throws SQLException {
        ParkingRecord record = new ParkingRecord(
                0, "2099-01-03 10:00:00", 101, 0);

        try {
            service.save(record, 100);
            fail("Expected invalid occupancy to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Occupied spaces"));
        }
    }

    @Test
    public void rejectIncorrectAvailableSpaces() throws SQLException {
        ParkingRecord record = new ParkingRecord(
                0, "2099-01-04 10:00:00", 60, 50);

        try {
            service.save(record, 100);
            fail("Expected incorrect available spaces to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Available spaces"));
        }
    }

    @Test
    public void rejectZeroTotalCapacity() throws SQLException {
        ParkingRecord record = new ParkingRecord(
                0, "2099-01-05 10:00:00", 0, 0);

        try {
            service.save(record, 0);
            fail("Expected zero capacity to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Total spaces"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectInvalidTimestampFormat() throws SQLException {
        ParkingRecord record = new ParkingRecord(
                0, "10/01/2026 10:00 AM", 60, 40);

        service.save(record, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectImpossibleTimestampDate() throws SQLException {
        ParkingRecord record = new ParkingRecord(
                0, "2026-02-30 10:00:00", 60, 40);

        service.save(record, 100);
    }
}