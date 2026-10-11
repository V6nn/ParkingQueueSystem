
package com.parking.optimization;

import com.parking.queueing.QueueingCalculator;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Calculates parking recommendations using queue observations
 * and the queueing calculator.
 */
public final class ParkingRecommendationCalculator {

    private ParkingRecommendationCalculator() {
        // Utility class.
    }

    /**
     * Recommends lanes based on the observed queue.
     * targetQueuePerLane defines the desired maximum queue per lane.
     */
    public static OptionalInt recommendLanes(
            String status,
            int physicalLaneCount,
            int activeLanes,
            Integer observedQueueLength,
            int targetQueuePerLane) {

        if (targetQueuePerLane <= 0) {
            throw new IllegalArgumentException(
                    "Target queue per lane must be positive.");
        }

        // Validate entrance information, including closed entrances.
        OptimizationRuleCalculator.limitRecommendedLanes(
                status, physicalLaneCount, activeLanes, 0);

        if ("CLOSED".equalsIgnoreCase(status)) {
            return OptionalInt.of(0);
        }

        if (!OptimizationRuleCalculator.hasValidQueueObservation(
                observedQueueLength)) {
            return OptionalInt.empty();
        }

        int proposedLanes = (int) Math.ceil(
                (double) observedQueueLength / targetQueuePerLane);

        int recommendedLanes =
                OptimizationRuleCalculator.limitRecommendedLanes(
                        status, physicalLaneCount, activeLanes,
                        proposedLanes);

        return OptionalInt.of(recommendedLanes);
    }

    /**
     * Estimates queue waiting time when the required observations exist.
     */
    public static OptionalDouble estimateWaitMinutes(
            Integer queueLength,
            int recommendedLanes,
            Double averageServiceTimeSeconds) {

        if (!OptimizationRuleCalculator.hasValidQueueObservation(
                queueLength)
                || recommendedLanes <= 0
                || averageServiceTimeSeconds == null
                || !Double.isFinite(averageServiceTimeSeconds)
                || averageServiceTimeSeconds <= 0) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(
                QueueingCalculator.estimateQueueWaitMinutes(
                        queueLength, recommendedLanes,
                        averageServiceTimeSeconds));
    }

    /**
     * Predicts queue length after a given number of minutes.
     */
    public static OptionalInt predictQueueLength(
            Integer currentQueueLength,
            double arrivalRatePerMinute,
            int recommendedLanes,
            Double averageServiceTimeSeconds,
            double predictionMinutes) {

        if (!OptimizationRuleCalculator.hasValidQueueObservation(
                currentQueueLength)
                || recommendedLanes < 0
                || averageServiceTimeSeconds == null
                || !Double.isFinite(averageServiceTimeSeconds)
                || averageServiceTimeSeconds <= 0
                || !Double.isFinite(arrivalRatePerMinute)
                || arrivalRatePerMinute < 0
                || !Double.isFinite(predictionMinutes)
                || predictionMinutes < 0) {
            return OptionalInt.empty();
        }

        double serviceRatePerMinute =
                QueueingCalculator.calculateServiceRate(
                        averageServiceTimeSeconds) * recommendedLanes;

        return OptionalInt.of(
                QueueingCalculator.predictQueueLength(
                        currentQueueLength,
                        arrivalRatePerMinute,
                        serviceRatePerMinute,
                        predictionMinutes));
    }

    /**
     * Limits proposed vehicle entries to available parking spaces.
     */
    public static int recommendEntryCapacity(
            int proposedCapacity,
            int availableSpaces) {

        return OptimizationRuleCalculator.limitRecommendedCapacity(
                proposedCapacity, availableSpaces);
    }
}
