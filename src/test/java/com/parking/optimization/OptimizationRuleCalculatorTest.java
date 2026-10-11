package com.parking.optimization;

import org.junit.Test;

import static org.junit.Assert.*;

public class OptimizationRuleCalculatorTest {

    @Test
    public void closedEntranceGetsZeroRecommendedLanes() {
        assertEquals(0,
                OptimizationRuleCalculator.limitRecommendedLanes(
                        "CLOSED", 3, 0, 2));
    }

    @Test
public void recommendationCanActivateAdditionalLanes() {
    assertEquals(3,
            OptimizationRuleCalculator.limitRecommendedLanes(
                    "OPEN", 4, 2, 3));
}

    @Test
    public void recommendedLanesDoNotExceedPhysicalLaneCount() {
        assertEquals(3,
                OptimizationRuleCalculator.limitRecommendedLanes(
                        "OPEN", 3, 3, 5));
    }

    @Test
    public void capacityDoesNotExceedAvailableSpaces() {
        assertEquals(8,
                OptimizationRuleCalculator.limitRecommendedCapacity(
                        12, 8));
    }

    @Test
    public void capacityIsZeroWhenNoSpacesAreAvailable() {
        assertEquals(0,
                OptimizationRuleCalculator.limitRecommendedCapacity(
                        5, 0));
    }

    @Test
    public void missingQueueObservationIsInvalid() {
        assertFalse(
                OptimizationRuleCalculator.hasValidQueueObservation(null));
    }

    @Test
    public void zeroQueueIsAValidObservation() {
        assertTrue(
                OptimizationRuleCalculator.hasValidQueueObservation(0));
    }

    @Test
    public void negativeQueueObservationIsInvalid() {
        assertFalse(
                OptimizationRuleCalculator.hasValidQueueObservation(-1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidEntranceStatus() {
        OptimizationRuleCalculator.limitRecommendedLanes(
                "UNKNOWN", 3, 2, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsActiveLanesAbovePhysicalLimit() {
        OptimizationRuleCalculator.limitRecommendedLanes(
                "OPEN", 2, 3, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeAvailableSpaces() {
        OptimizationRuleCalculator.limitRecommendedCapacity(5, -1);
    }
}
