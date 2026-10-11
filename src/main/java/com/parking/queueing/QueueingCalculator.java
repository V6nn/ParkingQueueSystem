
package com.parking.queueing;

/**
 * Basic queueing calculations for the parking system.
 * Time values are measured in seconds unless stated otherwise.
 */
public class QueueingCalculator {

    private QueueingCalculator() {
        // Utility class; no instances needed.
    }

    /**
     * Calculates the average time needed to serve one vehicle.
     */
    public static double calculateAverageServiceTime(
            double totalServiceTimeSeconds,
            int vehiclesServed) {

        if (!Double.isFinite(totalServiceTimeSeconds)
                || totalServiceTimeSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Total service time must be positive.");
        }

        if (vehiclesServed <= 0) {
            throw new IllegalArgumentException(
                    "Vehicles served must be greater than zero.");
        }

        double averageServiceTime =
                totalServiceTimeSeconds / vehiclesServed;

        if (!Double.isFinite(averageServiceTime)
                || averageServiceTime <= 0) {
            throw new IllegalArgumentException(
                    "Average service time is outside the supported numeric range.");
        }

        return averageServiceTime;
    }

    /**
     * Calculates the service rate of one lane in vehicles per minute.
     */
    public static double calculateServiceRate(
            double averageServiceTimeSeconds) {

        if (!Double.isFinite(averageServiceTimeSeconds)
                || averageServiceTimeSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Average service time must be a positive number.");
        }

        double serviceRate = 60.0 / averageServiceTimeSeconds;

        if (!Double.isFinite(serviceRate) || serviceRate <= 0) {
            throw new IllegalArgumentException(
                    "Average service time produces an invalid service rate.");
        }

        return serviceRate;
    }

    /**
     * Estimates minutes needed to serve the existing queue,
     * assuming no new arrivals.
     */
    public static double estimateQueueWaitMinutes(
            int queueLength,
            int activeLanes,
            double averageServiceTimeSeconds) {

        if (queueLength < 0) {
            throw new IllegalArgumentException(
                    "Queue length cannot be negative.");
        }

        if (activeLanes <= 0) {
            throw new IllegalArgumentException(
                    "Active lanes must be greater than zero.");
        }

        double serviceRate =
                calculateServiceRate(averageServiceTimeSeconds);

        double totalServiceRate = serviceRate * activeLanes;
        if (!Double.isFinite(totalServiceRate) || totalServiceRate <= 0) {
            throw new IllegalArgumentException(
                    "Combined service rate is outside the supported numeric range.");
        }

        double waitMinutes = queueLength / totalServiceRate;
        if (!Double.isFinite(waitMinutes)) {
            throw new IllegalArgumentException(
                    "Estimated waiting time is outside the supported numeric range.");
        }

        return waitMinutes;
    }

    /**
     * Calculates average vehicle arrivals per minute.
     */
    public static double calculateArrivalRate(
            int vehiclesArrived,
            double observationMinutes) {

        if (vehiclesArrived < 0) {
            throw new IllegalArgumentException(
                    "Number of arriving vehicles cannot be negative.");
        }

        if (!Double.isFinite(observationMinutes)
                || observationMinutes <= 0) {
            throw new IllegalArgumentException(
                    "Observation time must be a positive number.");
        }

        double arrivalRate = vehiclesArrived / observationMinutes;

        if (!Double.isFinite(arrivalRate)) {
            throw new IllegalArgumentException(
                    "Arrival rate is outside the supported numeric range.");
        }

        return arrivalRate;
    }

    /**
     * Predicts queue length after a given time period.
     * Service rate is the combined rate across all active lanes.
     */
    public static int predictQueueLength(
            int currentQueueLength,
            double arrivalRatePerMinute,
            double serviceRatePerMinute,
            double predictionMinutes) {

        if (currentQueueLength < 0) {
            throw new IllegalArgumentException(
                    "Queue length cannot be negative.");
        }

        if (!Double.isFinite(arrivalRatePerMinute)
                || arrivalRatePerMinute < 0) {
            throw new IllegalArgumentException(
                    "Arrival rate cannot be negative or invalid.");
        }

        if (!Double.isFinite(serviceRatePerMinute)
                || serviceRatePerMinute < 0) {
            throw new IllegalArgumentException(
                    "Service rate cannot be negative or invalid.");
        }

        if (!Double.isFinite(predictionMinutes)
                || predictionMinutes < 0) {
            throw new IllegalArgumentException(
                    "Prediction time cannot be negative or invalid.");
        }

        double predicted = currentQueueLength
                + (arrivalRatePerMinute - serviceRatePerMinute)
                * predictionMinutes;

        return (int) Math.ceil(Math.max(0, predicted));
    }
}
