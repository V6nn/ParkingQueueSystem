
package com.parking.services;

import com.parking.database.DatabaseInitializer;
import com.parking.models.Entrance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class EntranceServiceTest {

    private EntranceService service;
    private final List<Integer> testIds = new ArrayList<>();

    @Before
    public void setUp() throws SQLException {
        DatabaseInitializer.initialize();
        service = new EntranceService();
    }

    @After
    public void cleanUp() throws SQLException {
        for (int id : testIds) {
            service.delete(id);
        }
    }

    private Entrance saveTestEntrance(String name, int lanes,
                                      int activeLanes, String status)
            throws SQLException {
        Entrance entrance =
                new Entrance(0, name, lanes, activeLanes, status);
        service.save(entrance);
        testIds.add(entrance.getId());
        return entrance;
    }

    @Test
    public void saveAndFindEntrance() throws SQLException {
        Entrance entrance =
                saveTestEntrance("Test Entrance A", 3, 2, "OPEN");

        Optional<Entrance> result = service.findById(entrance.getId());

        assertTrue(result.isPresent());
        assertEquals("Test Entrance A", result.get().getName());
        assertEquals(3, result.get().getLaneCount());
        assertEquals(2, result.get().getActiveLanes());
        assertEquals("OPEN", result.get().getStatus());
    }

    @Test
    public void updateEntrance() throws SQLException {
        Entrance entrance =
                saveTestEntrance("Test Entrance B", 2, 1, "OPEN");

        entrance.setName("Updated Test Entrance B");
        entrance.setLaneCount(4);
        entrance.setActiveLanes(3);
        entrance.setStatus("CLOSED");

        assertTrue(service.update(entrance));

        Optional<Entrance> result = service.findById(entrance.getId());

        assertTrue(result.isPresent());
        assertEquals("Updated Test Entrance B", result.get().getName());
        assertEquals(4, result.get().getLaneCount());
        assertEquals(3, result.get().getActiveLanes());
        assertEquals("CLOSED", result.get().getStatus());
    }

    @Test
    public void deleteEntrance() throws SQLException {
        Entrance entrance =
                saveTestEntrance("Test Entrance C", 2, 1, "OPEN");

        assertTrue(service.delete(entrance.getId()));
        assertFalse(service.findById(entrance.getId()).isPresent());

        testIds.remove(Integer.valueOf(entrance.getId()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectZeroLanes() throws SQLException {
        service.save(new Entrance(0, "Invalid Entrance A", 0, 0, "OPEN"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectTooManyActiveLanes() throws SQLException {
        service.save(new Entrance(0, "Invalid Entrance B", 2, 3, "OPEN"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectInvalidStatus() throws SQLException {
        service.save(new Entrance(0, "Invalid Entrance C", 2, 1, "BUSY"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectUpdateWithInvalidId() throws SQLException {
        Entrance entrance =
                new Entrance(0, "Invalid ID Entrance", 2, 1, "OPEN");

        service.update(entrance);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectFindByInvalidId() throws SQLException {
        service.findById(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectDeleteWithInvalidId() throws SQLException {
        service.delete(0);
    }
}