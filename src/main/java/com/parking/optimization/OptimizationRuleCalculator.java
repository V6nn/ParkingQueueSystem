package com.parking.optimization;

/**
 * Applies basic safety constraints to parking entrance recommendations.
 *
 * This class does not access the database or predict queue lengths.
 * It only validates and limits a proposed lane and entry-capacity
 * recommendation.
 */
public final class OptimizationRuleCalculator {

    private OptimizationRuleCalculator() {
        // Utility class.
    }

    /**
     * Returns the allowed recommended lane count.
     *
     * Closed entrances and entrances with no available active lanes
     * cannot be recommended for new entry traffic.
     */
    public static int limitRecommendedLanes(
            String status,
            int physicalLaneCount,
            int activeLanes,
            int proposedLanes) {

        if (status == null
                || !(status.equalsIgnoreCase("OPEN")
                || status.equalsIgnoreCase("CLOSED"))) {
            throw new IllegalArgumentException(
                    "Status must be OPEN or CLOSED.");
        }

        if (physicalLaneCount <= 0) {
            throw new IllegalArgumentException(
                    "Physical lane count must be positive.");
        }

        if (activeLanes < 0 || activeLanes > physicalLaneCount) {
            throw new IllegalArgumentException(
                    "Active lanes must be between zero and physical lane count.");
        }

        if (proposedLanes < 0) {
            throw new IllegalArgumentException(
                    "Proposed lanes cannot be negative.");
        }

        if (status.equalsIgnoreCase("CLOSED")) {
    return 0;
}

return Math.min(proposedLanes, physicalLaneCount);
    }

    /**
     * Limits a proposed number of vehicle entries to available spaces.
     */
    public static int limitRecommendedCapacity(
            int proposedCapacity,
            int availableSpaces) {

        if (proposedCapacity < 0) {
            throw new IllegalArgumentException(
                    "Proposed capacity cannot be negative.");
        }

        if (availableSpaces < 0) {
            throw new IllegalArgumentException(
                    "Available spaces cannot be negative.");
        }

        return Math.min(proposedCapacity, availableSpaces);
    }

    /**
     * Indicates whether a queue observation is available.
     *
     * A missing observation must not be interpreted as an empty queue.
     */
    public static boolean hasValidQueueObservation(
            Integer observedQueueLength) {

        return observedQueueLength != null && observedQueueLength >= 0;
    }
}
