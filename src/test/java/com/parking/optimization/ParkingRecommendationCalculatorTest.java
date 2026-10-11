
package com.parking.optimization;

import org.junit.Test;

import java.util.OptionalDouble;
import java.util.OptionalInt;

import static org.junit.Assert.*;

public class ParkingRecommendationCalculatorTest {

    @Test
    public void recommendsMoreLanesForLongerQueue() {
        OptionalInt result =
                ParkingRecommendationCalculator.recommendLanes(
                        "OPEN", 4, 2, 12, 4);

        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void zeroQueueDoesNotRecommendOpeningLanes() {
        OptionalInt result =
                ParkingRecommendationCalculator.recommendLanes(
                        "OPEN", 4, 2, 0, 4);

        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void closedEntranceGetsZeroLanes() {
        OptionalInt result =
                ParkingRecommendationCalculator.recommendLanes(
                        "CLOSED", 4, 0, 12, 4);

        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void missingQueueProducesNoLaneRecommendation() {
        OptionalInt result =
                ParkingRecommendationCalculator.recommendLanes(
                        "OPEN", 4, 2, null, 4);

        assertFalse(result.isPresent());
    }

    @Test
    public void estimatesWaitUsingRecommendedLanes() {
        OptionalDouble result =
                ParkingRecommendationCalculator.estimateWaitMinutes(
                        12, 2, 30.0);

        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.0001);
    }

    @Test
    public void missingQueueProducesNoWaitEstimate() {
        OptionalDouble result =
                ParkingRecommendationCalculator.estimateWaitMinutes(
                        null, 2, 30.0);

        assertFalse(result.isPresent());
    }

    @Test
    public void predictsQueueUsingCombinedServiceRate() {
        OptionalInt result =
                ParkingRecommendationCalculator.predictQueueLength(
                        10, 1.0, 2, 30.0, 5.0);

        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void entryCapacityDoesNotExceedAvailableSpaces() {
        assertEquals(8,
                ParkingRecommendationCalculator.recommendEntryCapacity(
                        12, 8));
    }

    @Test
    public void predictedQueueCanIncludeArrivalsWhenNoLanesAreOpen() {
        OptionalInt result =
                ParkingRecommendationCalculator.predictQueueLength(
                        0, 2.0, 0, 30.0, 5.0);

        assertTrue(result.isPresent());
        assertEquals(10, result.getAsInt());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidLaneCount() {
        ParkingRecommendationCalculator.recommendLanes(
                "OPEN", 0, 0, 4, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeRecommendedCapacity() {
        ParkingRecommendationCalculator.recommendEntryCapacity(-1, 5);
    }
}
