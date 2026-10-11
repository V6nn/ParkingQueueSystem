
package com.parking.services;

import com.parking.database.DatabaseConnection;
import com.parking.database.DatabaseInitializer;
import com.parking.models.Entrance;
import com.parking.models.EntranceRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class EntranceRecordServiceTest {

    private EntranceRecordService service;
    private EntranceService entranceService;
    private int entranceId;
    private int firstRecordId;
    private int secondRecordId;

    @Before
    public void setUp() throws SQLException {
        DatabaseInitializer.initialize();

        service = new EntranceRecordService();
        entranceService = new EntranceService();

        Entrance entrance = new Entrance(
                0, "Test Entrance Record", 2, 2, "OPEN");

        entranceService.save(entrance);
        entranceId = entrance.getId();

        firstRecordId = 0;
        secondRecordId = 0;
    }

    @After
    public void cleanUp() throws SQLException {
        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM entrance_records WHERE id = ?")) {

            if (firstRecordId > 0) {
                statement.setInt(1, firstRecordId);
                statement.executeUpdate();
            }

            if (secondRecordId > 0) {
                statement.setInt(1, secondRecordId);
                statement.executeUpdate();
            }
        }

        if (entranceId > 0) {
            entranceService.delete(entranceId);
        }
    }

    @Test
    public void saveAndFindLatestRecord() throws SQLException {
        EntranceRecord record = new EntranceRecord(
                0, entranceId, "2099-01-01 10:00:00",
                5, 10, 2.5);

        service.save(record);
        firstRecordId = record.getId();

        assertTrue(firstRecordId > 0);

        Optional<EntranceRecord> result =
                service.findLatestByEntranceId(entranceId);

        assertTrue(result.isPresent());
        assertEquals(firstRecordId, result.get().getId());
        assertEquals(5, result.get().getQueueLength());
        assertEquals(10, result.get().getVehiclesEntered());
        assertEquals(2.5, result.get().getAverageServiceTime(), 0.001);
    }

    @Test
    public void findAllReturnsSavedRecord() throws SQLException {
        EntranceRecord record = new EntranceRecord(
                0, entranceId, "2099-01-02 10:00:00",
                3, 8, 1.5);

        service.save(record);
        firstRecordId = record.getId();

        List<EntranceRecord> records = service.findAll();

        assertTrue(records.stream().anyMatch(
                item -> item.getId() == firstRecordId));
    }

    @Test
    public void findByEntranceIdReturnsMatchingRecords()
            throws SQLException {

        EntranceRecord record = new EntranceRecord(
                0, entranceId, "2099-01-03 10:00:00",
                4, 6, 2.0);

        service.save(record);
        firstRecordId = record.getId();

        List<EntranceRecord> records =
                service.findByEntranceId(entranceId);

        assertTrue(records.stream().anyMatch(
                item -> item.getId() == firstRecordId));

        assertTrue(records.stream().allMatch(
                item -> item.getEntranceId() == entranceId));
    }

    @Test
    public void latestRecordIsEmptyWhenNoRecordsExist()
            throws SQLException {

        Optional<EntranceRecord> result =
                service.findLatestByEntranceId(entranceId);

        assertFalse(result.isPresent());
    }

    @Test
    public void rejectInvalidEntranceId() throws SQLException {
        EntranceRecord record = new EntranceRecord(
                0, 0, "2099-01-04 10:00:00",
                2, 5, 1.0);

        try {
            service.save(record);
            fail("Expected invalid entrance ID to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("entrance ID"));
        }
    }

    @Test
    public void rejectNegativeQueueLength() throws SQLException {
        EntranceRecord record = new EntranceRecord(
                0, entranceId, "2099-01-05 10:00:00",
                -1, 5, 1.0);

        try {
            service.save(record);
            fail("Expected negative queue length to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Queue length"));
        }
    }

    @Test
    public void rejectNegativeAverageServiceTime()
            throws SQLException {

        EntranceRecord record = new EntranceRecord(
                0, entranceId, "2099-01-06 10:00:00",
                2, 5, -1.0);

        try {
            service.save(record);
            fail("Expected negative service time to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains(
                    "Average service time"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectInvalidTimestampFormat() throws SQLException {
        EntranceRecord record = new EntranceRecord(
                0, entranceId, "10/01/2026 10:00 AM",
                2, 5, 1.0);

        service.save(record);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectImpossibleTimestampDate() throws SQLException {
        EntranceRecord record = new EntranceRecord(
                0, entranceId, "2026-02-30 10:00:00",
                2, 5, 1.0);

        service.save(record);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectFindByInvalidEntranceId() throws SQLException {
        service.findByEntranceId(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectFindLatestByInvalidEntranceId() throws SQLException {
        service.findLatestByEntranceId(0);
    }
}