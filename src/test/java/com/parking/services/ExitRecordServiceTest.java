
package com.parking.services;

import com.parking.database.DatabaseConnection;
import com.parking.database.DatabaseInitializer;
import com.parking.models.Exit;
import com.parking.models.ExitRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class ExitRecordServiceTest {

    private ExitRecordService service;
    private ExitService exitService;
    private int exitId;
    private int firstRecordId;
    private int secondRecordId;

    @Before
    public void setUp() throws SQLException {
        DatabaseInitializer.initialize();

        service = new ExitRecordService();
        exitService = new ExitService();

        Exit exit = new Exit(
                0, "Test Exit Record", 2, 2, "OPEN");

        exitService.save(exit);
        exitId = exit.getId();

        firstRecordId = 0;
        secondRecordId = 0;
    }

    @After
    public void cleanUp() throws SQLException {
        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM exit_records WHERE id = ?")) {

            if (firstRecordId > 0) {
                statement.setInt(1, firstRecordId);
                statement.executeUpdate();
            }

            if (secondRecordId > 0) {
                statement.setInt(1, secondRecordId);
                statement.executeUpdate();
            }
        }

        if (exitId > 0) {
            exitService.delete(exitId);
        }
    }

    @Test
    public void saveAndFindLatestRecord() throws SQLException {
        ExitRecord record = new ExitRecord(
                0, exitId, "2099-01-01 10:00:00", 12);

        service.save(record);
        firstRecordId = record.getId();

        assertTrue(firstRecordId > 0);

        Optional<ExitRecord> result =
                service.findLatestByExitId(exitId);

        assertTrue(result.isPresent());
        assertEquals(firstRecordId, result.get().getId());
        assertEquals(12, result.get().getVehiclesExited());
    }

    @Test
    public void findAllReturnsSavedRecord() throws SQLException {
        ExitRecord record = new ExitRecord(
                0, exitId, "2099-01-02 10:00:00", 8);

        service.save(record);
        firstRecordId = record.getId();

        List<ExitRecord> records = service.findAll();

        assertTrue(records.stream().anyMatch(
                item -> item.getId() == firstRecordId));
    }

    @Test
    public void findByExitIdReturnsMatchingRecords()
            throws SQLException {

        ExitRecord record = new ExitRecord(
                0, exitId, "2099-01-03 10:00:00", 15);

        service.save(record);
        firstRecordId = record.getId();

        List<ExitRecord> records = service.findByExitId(exitId);

        assertTrue(records.stream().anyMatch(
                item -> item.getId() == firstRecordId));

        assertTrue(records.stream().allMatch(
                item -> item.getExitId() == exitId));
    }

    @Test
    public void latestRecordIsEmptyWhenNoRecordsExist()
            throws SQLException {

        Optional<ExitRecord> result =
                service.findLatestByExitId(exitId);

        assertFalse(result.isPresent());
    }

    @Test
    public void rejectInvalidExitId() throws SQLException {
        ExitRecord record = new ExitRecord(
                0, 0, "2099-01-04 10:00:00", 5);

        try {
            service.save(record);
            fail("Expected invalid exit ID to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("exit ID"));
        }
    }

    @Test
    public void rejectNegativeVehiclesExited()
            throws SQLException {

        ExitRecord record = new ExitRecord(
                0, exitId, "2099-01-05 10:00:00", -1);

        try {
            service.save(record);
            fail("Expected negative vehicle count to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains(
                    "Vehicles exited"));
        }
    }

    @Test
    public void rejectBlankTimestamp() throws SQLException {
        ExitRecord record = new ExitRecord(
                0, exitId, "   ", 5);

        try {
            service.save(record);
            fail("Expected blank timestamp to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains(
                    "timestamp"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectInvalidTimestampFormat() throws SQLException {
        ExitRecord record = new ExitRecord(
                0, exitId, "10/01/2026 10:00 AM", 5);

        service.save(record);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectImpossibleTimestampDate() throws SQLException {
        ExitRecord record = new ExitRecord(
                0, exitId, "2026-02-30 10:00:00", 5);

        service.save(record);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectFindByInvalidExitId() throws SQLException {
        service.findByExitId(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectFindLatestByInvalidExitId() throws SQLException {
        service.findLatestByExitId(0);
    }
}