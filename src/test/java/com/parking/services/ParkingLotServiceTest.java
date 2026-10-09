
package com.parking.services;

import com.parking.database.DatabaseConnection;
import com.parking.database.DatabaseInitializer;
import com.parking.models.ParkingLot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class ParkingLotServiceTest {

    private ParkingLotService service;
    private int firstParkingLotId;
    private int secondParkingLotId;

    @Before
    public void setUp() {
        DatabaseInitializer.initialize();
        service = new ParkingLotService();

        firstParkingLotId = 0;
        secondParkingLotId = 0;
    }

    @After
    public void cleanUp() throws SQLException {
        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM parking_lot WHERE id = ?")) {

            if (firstParkingLotId > 0) {
                statement.setInt(1, firstParkingLotId);
                statement.executeUpdate();
            }

            if (secondParkingLotId > 0) {
                statement.setInt(1, secondParkingLotId);
                statement.executeUpdate();
            }
        }
    }

    @Test
    public void saveAndFindParkingLotById() throws SQLException {
        ParkingLot parkingLot = new ParkingLot(
                0, "Test Parking Lot", 100,
                "06:00", "22:00");

        int id = service.save(parkingLot);
        firstParkingLotId = id;

        assertTrue(id > 0);
        assertEquals(id, parkingLot.getId());

        Optional<ParkingLot> result = service.findById(id);

        assertTrue(result.isPresent());
        assertEquals("Test Parking Lot", result.get().getName());
        assertEquals(100, result.get().getTotalSpaces());
        assertEquals("06:00", result.get().getOperatingStart());
        assertEquals("22:00", result.get().getOperatingEnd());
    }

    @Test
    public void updateExistingParkingLot() throws SQLException {
        ParkingLot parkingLot = new ParkingLot(
                0, "Original Parking Lot", 100,
                "06:00", "22:00");

        firstParkingLotId = service.save(parkingLot);

        parkingLot.setName("Updated Parking Lot");
        parkingLot.setTotalSpaces(150);
        parkingLot.setOperatingStart("05:00");
        parkingLot.setOperatingEnd("23:00");

        assertTrue(service.update(parkingLot));

        Optional<ParkingLot> result =
                service.findById(firstParkingLotId);

        assertTrue(result.isPresent());
        assertEquals("Updated Parking Lot", result.get().getName());
        assertEquals(150, result.get().getTotalSpaces());
        assertEquals("05:00", result.get().getOperatingStart());
        assertEquals("23:00", result.get().getOperatingEnd());
    }

    @Test
    public void updateReturnsFalseForMissingParkingLot()
            throws SQLException {

        ParkingLot parkingLot = new ParkingLot(
                999999, "Missing Parking Lot", 100,
                "06:00", "22:00");

        assertFalse(service.update(parkingLot));
    }

    @Test
    public void findAllReturnsSavedParkingLots() throws SQLException {
        ParkingLot first = new ParkingLot(
                0, "Test Lot A", 100, "06:00", "22:00");

        ParkingLot second = new ParkingLot(
                0, "Test Lot B", 200, "07:00", "23:00");

        firstParkingLotId = service.save(first);
        secondParkingLotId = service.save(second);

        List<ParkingLot> parkingLots = service.findAll();

        assertTrue(parkingLots.stream().anyMatch(
                lot -> lot.getId() == firstParkingLotId));

        assertTrue(parkingLots.stream().anyMatch(
                lot -> lot.getId() == secondParkingLotId));
    }

    @Test
    public void findByIdReturnsEmptyForMissingParkingLot()
            throws SQLException {

        Optional<ParkingLot> result = service.findById(999999);

        assertFalse(result.isPresent());
    }

    @Test
    public void rejectBlankParkingLotName() throws SQLException {
        ParkingLot parkingLot = new ParkingLot(
                0, "   ", 100, "06:00", "22:00");

        try {
            service.save(parkingLot);
            fail("Expected blank name to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("name"));
        }
    }

    @Test
    public void rejectZeroParkingSpaces() throws SQLException {
        ParkingLot parkingLot = new ParkingLot(
                0, "Test Lot", 0, "06:00", "22:00");

        try {
            service.save(parkingLot);
            fail("Expected zero capacity to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("spaces"));
        }
    }

    @Test
    public void rejectNegativeParkingSpaces() throws SQLException {
        ParkingLot parkingLot = new ParkingLot(
                0, "Test Lot", -10, "06:00", "22:00");

        try {
            service.save(parkingLot);
            fail("Expected negative capacity to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("spaces"));
        }
    }

    @Test
    public void rejectInvalidOperatingStartTime() throws SQLException {
        ParkingLot parkingLot = new ParkingLot(
                0, "Test Lot", 100, "25:90", "22:00");

        try {
            service.save(parkingLot);
            fail("Expected invalid start time to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("HH:mm"));
        }
    }

    @Test
    public void rejectMissingOperatingEndTime() throws SQLException {
        ParkingLot parkingLot = new ParkingLot(
                0, "Test Lot", 100, "06:00", "");

        try {
            service.save(parkingLot);
            fail("Expected missing end time to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("required"));
        }
    }

    @Test
    public void rejectInvalidDataWhenUpdating() throws SQLException {
        ParkingLot parkingLot = new ParkingLot(
                999999, "Test Lot", 0, "06:00", "22:00");

        try {
            service.update(parkingLot);
            fail("Expected invalid capacity to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("spaces"));
        }
    }
}