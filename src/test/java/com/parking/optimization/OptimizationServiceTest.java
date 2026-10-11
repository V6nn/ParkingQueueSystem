package com.parking.optimization;

import com.parking.models.Entrance;
import com.parking.models.EntranceRecord;
import com.parking.models.ParkingRecord;
import com.parking.services.EntranceRecordService;
import com.parking.services.EntranceService;
import com.parking.services.ParkingRecordService;

import org.junit.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.Assert.*;

public class OptimizationServiceTest {

    private static class FakeEntranceService extends EntranceService {
        private Entrance entrance;

        @Override
        public Optional<Entrance> findById(int id) {
            if (entrance != null && entrance.getId() == id) {
                return Optional.of(entrance);
            }
            return Optional.empty();
        }
    }

    private static class FakeEntranceRecordService
            extends EntranceRecordService {
        private EntranceRecord record;

        @Override
        public Optional<EntranceRecord> findLatestByEntranceId(int id) {
            if (record != null && record.getEntranceId() == id) {
                return Optional.of(record);
            }
            return Optional.empty();
        }
    }

    private static class FakeParkingRecordService
            extends ParkingRecordService {
        private ParkingRecord record;

        @Override
        public ParkingRecord findLatest() {
            return record;
        }
    }

    @Test
    public void openEntranceProducesLaneRecommendation()
            throws SQLException {
        FakeEntranceService entrances = new FakeEntranceService();
        FakeEntranceRecordService records =
                new FakeEntranceRecordService();
        FakeParkingRecordService parking =
                new FakeParkingRecordService();

        entrances.entrance =
                new Entrance(1, "Main Entrance", 4, 2, "OPEN");
        records.record =
                new EntranceRecord(
                        1, 1, "2026-10-11 10:00:00", 12, 20, 30.0,
                        2.5);

        OptimizationService service =
                new OptimizationService(entrances, records, parking);

        Optional<OptimizationService.OptimizationRecommendation> result =
                service.generateRecommendation(1, 10, 4);

        assertTrue(result.isPresent());
        assertEquals(3,
                result.get().getRecommendedLanes().getAsInt());
        assertTrue(
                result.get().getEstimatedWaitMinutes().isPresent());
        assertTrue(
                result.get().getPredictedQueueLength().isPresent());
        assertEquals(22,
                result.get().getPredictedQueueLength().getAsInt());
    }

    @Test
    public void closedEntranceRecommendsZeroLanes()
            throws SQLException {
        FakeEntranceService entrances = new FakeEntranceService();
        FakeEntranceRecordService records =
                new FakeEntranceRecordService();
        FakeParkingRecordService parking =
                new FakeParkingRecordService();

        entrances.entrance =
                new Entrance(1, "Closed Entrance", 4, 0, "CLOSED");

        OptimizationService service =
                new OptimizationService(entrances, records, parking);

        Optional<OptimizationService.OptimizationRecommendation> result =
                service.generateRecommendation(1, 10, 4);

        assertTrue(result.isPresent());
        assertEquals(0,
                result.get().getRecommendedLanes().getAsInt());
        assertFalse(
                result.get().getEstimatedWaitMinutes().isPresent());
    }

    @Test
    public void missingEntranceReturnsEmptyResult()
            throws SQLException {
        OptimizationService service =
                new OptimizationService(
                        new FakeEntranceService(),
                        new FakeEntranceRecordService(),
                        new FakeParkingRecordService());

        Optional<OptimizationService.OptimizationRecommendation> result =
                service.generateRecommendation(99, 10, 4);

        assertFalse(result.isPresent());
    }

    @Test
    public void missingParkingRecordDoesNotPreventLaneRecommendation()
            throws SQLException {
        FakeEntranceService entrances = new FakeEntranceService();
        FakeEntranceRecordService records =
                new FakeEntranceRecordService();
        FakeParkingRecordService parking =
                new FakeParkingRecordService();

        entrances.entrance =
                new Entrance(1, "Main Entrance", 4, 2, "OPEN");
        records.record =
                new EntranceRecord(
                        1, 1, "2026-10-11 10:00:00", 8, 3, 30.0);

        OptimizationService service =
                new OptimizationService(entrances, records, parking);

        Optional<OptimizationService.OptimizationRecommendation> result =
                service.generateRecommendation(1, 10, 4);

        assertTrue(result.isPresent());
        assertTrue(
                result.get().getRecommendedLanes().isPresent());
        assertFalse(
                result.get().getRecommendedCapacity().isPresent());
    }

    @Test
    public void missingObservationDurationDoesNotProduceQueuePrediction()
            throws SQLException {
        FakeEntranceService entrances = new FakeEntranceService();
        FakeEntranceRecordService records =
                new FakeEntranceRecordService();
        FakeParkingRecordService parking =
                new FakeParkingRecordService();

        entrances.entrance =
                new Entrance(1, "Main Entrance", 4, 2, "OPEN");
        records.record =
                new EntranceRecord(
                        1, 1, "2026-10-11 10:00:00", 8, 3, 30.0);

        OptimizationService service =
                new OptimizationService(entrances, records, parking);

        Optional<OptimizationService.OptimizationRecommendation> result =
                service.generateRecommendation(1, 10, 4);

        assertTrue(result.isPresent());
        assertFalse(
                result.get().getPredictedQueueLength().isPresent());
    }

    @Test
    public void missingServiceTimeDoesNotProduceWaitOrQueuePrediction()
            throws SQLException {
        FakeEntranceService entrances = new FakeEntranceService();
        FakeEntranceRecordService records =
                new FakeEntranceRecordService();
        FakeParkingRecordService parking =
                new FakeParkingRecordService();

        entrances.entrance =
                new Entrance(1, "Main Entrance", 4, 2, "OPEN");
        records.record =
                new EntranceRecord(
                        1, 1, "2026-10-11 10:00:00", 8, 3, null, 2.5);

        OptimizationService service =
                new OptimizationService(entrances, records, parking);

        Optional<OptimizationService.OptimizationRecommendation> result =
                service.generateRecommendation(1, 10, 4);

        assertTrue(result.isPresent());
        assertFalse(result.get().getEstimatedWaitMinutes().isPresent());
        assertFalse(result.get().getPredictedQueueLength().isPresent());
    }
}