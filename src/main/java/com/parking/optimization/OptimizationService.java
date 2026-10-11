package com.parking.optimization;

import com.parking.models.Entrance;
import com.parking.models.EntranceRecord;
import com.parking.models.ParkingRecord;
import com.parking.services.EntranceRecordService;
import com.parking.services.EntranceService;
import com.parking.services.ParkingRecordService;
import com.parking.models.ParkingLot;
import com.parking.services.ParkingLotService;
import com.parking.queueing.QueueingCalculator;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Connects parking data services to the optimization calculators.
 */
public class OptimizationService {

    private static final double QUEUE_PREDICTION_MINUTES = 5.0;

    private final EntranceService entranceService;
    private final EntranceRecordService entranceRecordService;
    private final ParkingRecordService parkingRecordService;
    private final ParkingLotService parkingLotService;

public OptimizationService() {
    this(
            new EntranceService(),
            new EntranceRecordService(),
            new ParkingRecordService(),
            new ParkingLotService()
    );
}

public OptimizationService(
        EntranceService entranceService,
        EntranceRecordService entranceRecordService,
        ParkingRecordService parkingRecordService) {

    this(
            entranceService,
            entranceRecordService,
            parkingRecordService,
            new ParkingLotService()
    );
}

public OptimizationService(
        EntranceService entranceService,
        EntranceRecordService entranceRecordService,
        ParkingRecordService parkingRecordService,
        ParkingLotService parkingLotService) {

    if (entranceService == null
            || entranceRecordService == null
            || parkingRecordService == null
            || parkingLotService == null) {
        throw new IllegalArgumentException(
                "Services cannot be null.");
    }

    this.entranceService = entranceService;
    this.entranceRecordService = entranceRecordService;
    this.parkingRecordService = parkingRecordService;
    this.parkingLotService = parkingLotService;
}

    /**
     * Generates recommendations using the latest saved records.
     *
     * Returns Optional.empty() if the entrance does not exist.
     */
    public Optional<OptimizationRecommendation> generateRecommendation(
            int entranceId,
            int proposedCapacity,
            int targetQueuePerLane) throws SQLException {

        if (entranceId <= 0) {
            throw new IllegalArgumentException(
                    "Entrance ID must be greater than zero.");
        }

        if (proposedCapacity < 0) {
            throw new IllegalArgumentException(
                    "Proposed capacity cannot be negative.");
        }

        Optional<Entrance> entranceResult =
                entranceService.findById(entranceId);

        if (entranceResult.isEmpty()) {
            return Optional.empty();
        }

        Entrance entrance = entranceResult.get();

        Optional<EntranceRecord> recordResult =
                entranceRecordService.findLatestByEntranceId(entranceId);

        EntranceRecord record = recordResult.orElse(null);

        ParkingRecord parkingRecord =
                parkingRecordService.findLatest();

        Integer queueLength = record == null
                ? null
                : record.getQueueLength();

        OptionalInt recommendedLanes =
                ParkingRecommendationCalculator.recommendLanes(
                        entrance.getStatus(),
                        entrance.getLaneCount(),
                        entrance.getActiveLanes(),
                        queueLength,
                        targetQueuePerLane
                );

        OptionalDouble estimatedWait = OptionalDouble.empty();
        OptionalInt predictedQueueLength = OptionalInt.empty();

        if (record != null && recommendedLanes.isPresent()) {
            estimatedWait =
                    ParkingRecommendationCalculator.estimateWaitMinutes(
                            queueLength,
                            recommendedLanes.getAsInt(),
                            record.getAverageServiceTime()
                    );

            Double observationDuration =
                    record.getObservationDurationMinutes();
            if (observationDuration != null) {
                double arrivalRate = QueueingCalculator.calculateArrivalRate(
                        record.getVehiclesEntered(), observationDuration);
                predictedQueueLength =
                        ParkingRecommendationCalculator.predictQueueLength(
                                queueLength,
                                arrivalRate,
                                recommendedLanes.getAsInt(),
                                record.getAverageServiceTime(),
                                QUEUE_PREDICTION_MINUTES);
            }
        }

        OptionalInt recommendedCapacity = OptionalInt.empty();

        if (parkingRecord != null) {
            List<ParkingLot> parkingLots =
                    parkingLotService.findAll();

            if (!parkingLots.isEmpty()) {
                int totalSpaces =
                        parkingLots.get(0).getTotalSpaces();

                int occupiedSpaces =
                        parkingRecord.getOccupiedSpaces();

                // Do not recommend entry capacity when the
                // saved occupancy exceeds the configured capacity.
                if (occupiedSpaces >= 0
                        && occupiedSpaces <= totalSpaces) {

                    int availableSpaces =
                            totalSpaces - occupiedSpaces;

                    recommendedCapacity = OptionalInt.of(
                            ParkingRecommendationCalculator
                                    .recommendEntryCapacity(
                                            proposedCapacity,
                                            availableSpaces));
                }
            }
        }

        return Optional.of(new OptimizationRecommendation(
                entrance.getId(),
                entrance.getName(),
                queueLength,
                record == null ? null : record.getRecordedAt(),
                recommendedLanes,
                estimatedWait,
                predictedQueueLength,
                recommendedCapacity,
                parkingRecord == null
                        ? null
                        : parkingRecord.getRecordedAt()
        ));
    }

    /**
     * Holds the result of one optimization calculation.
     */
    public static final class OptimizationRecommendation {

        private final int entranceId;
        private final String entranceName;
        private final Integer observedQueueLength;
        private final String queueRecordedAt;
        private final OptionalInt recommendedLanes;
        private final OptionalDouble estimatedWaitMinutes;
        private final OptionalInt predictedQueueLength;
        private final OptionalInt recommendedCapacity;
        private final String parkingRecordedAt;

        private OptimizationRecommendation(
                int entranceId,
                String entranceName,
                Integer observedQueueLength,
                String queueRecordedAt,
                OptionalInt recommendedLanes,
                OptionalDouble estimatedWaitMinutes,
                OptionalInt predictedQueueLength,
                OptionalInt recommendedCapacity,
                String parkingRecordedAt) {

            this.entranceId = entranceId;
            this.entranceName = entranceName;
            this.observedQueueLength = observedQueueLength;
            this.queueRecordedAt = queueRecordedAt;
            this.recommendedLanes = recommendedLanes;
            this.estimatedWaitMinutes = estimatedWaitMinutes;
            this.predictedQueueLength = predictedQueueLength;
            this.recommendedCapacity = recommendedCapacity;
            this.parkingRecordedAt = parkingRecordedAt;
        }

        public int getEntranceId() {
            return entranceId;
        }

        public String getEntranceName() {
            return entranceName;
        }

        public Integer getObservedQueueLength() {
            return observedQueueLength;
        }

        public String getQueueRecordedAt() {
            return queueRecordedAt;
        }

        public OptionalInt getRecommendedLanes() {
            return recommendedLanes;
        }

        public OptionalDouble getEstimatedWaitMinutes() {
            return estimatedWaitMinutes;
        }

        public OptionalInt getPredictedQueueLength() {
            return predictedQueueLength;
        }

        public OptionalInt getRecommendedCapacity() {
            return recommendedCapacity;
        }

        public String getParkingRecordedAt() {
            return parkingRecordedAt;
        }
    }
}
