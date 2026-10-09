
package com.parking.services;

import com.parking.database.DatabaseInitializer;
import com.parking.models.Exit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class ExitServiceTest {

    private ExitService service;
    private final List<Integer> testIds = new ArrayList<>();

    @Before
    public void setUp() {
        DatabaseInitializer.initialize();
        service = new ExitService();
    }

    @After
    public void cleanUp() throws SQLException {
        for (int id : testIds) {
            service.delete(id);
        }
    }

    private Exit saveTestExit(String name, int lanes,
                              int activeLanes, String status)
            throws SQLException {
        Exit exit = new Exit(0, name, lanes, activeLanes, status);
        service.save(exit);
        testIds.add(exit.getId());
        return exit;
    }

    @Test
    public void saveAndFindExit() throws SQLException {
        Exit exit = saveTestExit("Test Exit A", 3, 2, "OPEN");

        Optional<Exit> result = service.findById(exit.getId());

        assertTrue(result.isPresent());
        assertEquals("Test Exit A", result.get().getName());
        assertEquals(3, result.get().getLaneCount());
        assertEquals(2, result.get().getActiveLanes());
        assertEquals("OPEN", result.get().getStatus());
    }

    @Test
    public void updateExit() throws SQLException {
        Exit exit = saveTestExit("Test Exit B", 2, 1, "OPEN");

        exit.setName("Updated Test Exit B");
        exit.setLaneCount(4);
        exit.setActiveLanes(3);
        exit.setStatus("CLOSED");

        assertTrue(service.update(exit));

        Optional<Exit> result = service.findById(exit.getId());

        assertTrue(result.isPresent());
        assertEquals("Updated Test Exit B", result.get().getName());
        assertEquals(4, result.get().getLaneCount());
        assertEquals(3, result.get().getActiveLanes());
        assertEquals("CLOSED", result.get().getStatus());
    }

    @Test
    public void deleteExit() throws SQLException {
        Exit exit = saveTestExit("Test Exit C", 2, 1, "OPEN");

        assertTrue(service.delete(exit.getId()));
        assertFalse(service.findById(exit.getId()).isPresent());

        testIds.remove(Integer.valueOf(exit.getId()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectZeroLanes() throws SQLException {
        service.save(new Exit(0, "Invalid Exit A", 0, 0, "OPEN"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectTooManyActiveLanes() throws SQLException {
        service.save(new Exit(0, "Invalid Exit B", 2, 3, "OPEN"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectInvalidStatus() throws SQLException {
        service.save(new Exit(0, "Invalid Exit C", 2, 1, "BUSY"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectUpdateWithInvalidId() throws SQLException {
        Exit exit = new Exit(0, "Invalid ID Exit", 2, 1, "OPEN");

        service.update(exit);
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