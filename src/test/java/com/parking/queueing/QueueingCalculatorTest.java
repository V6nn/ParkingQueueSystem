package com.parking.queueing;

import org.junit.Test;
import static org.junit.Assert.*;

public class QueueingCalculatorTest {

@Test
public void calculatesServiceRate() {
    double result = QueueingCalculator.calculateServiceRate(30);
    assertEquals(2.0, result, 0.001);
}

@Test
public void estimatesQueueWaitTime() {
    double result =
            QueueingCalculator.estimateQueueWaitMinutes(12, 2, 30);
    assertEquals(3.0, result, 0.001);
}

@Test(expected = IllegalArgumentException.class)
public void rejectsZeroServiceTime() {
    QueueingCalculator.calculateServiceRate(0);
}

@Test(expected = IllegalArgumentException.class)
public void rejectsInfiniteServiceTime() {
    QueueingCalculator.calculateServiceRate(Double.POSITIVE_INFINITY);
}

@Test(expected = IllegalArgumentException.class)
public void rejectsServiceTimeThatOverflowsServiceRate() {
    QueueingCalculator.calculateServiceRate(Double.MIN_VALUE);
}
@Test
public void calculatesArrivalRate() {
    double result =
            QueueingCalculator.calculateArrivalRate(20, 10);

    assertEquals(2.0, result, 0.001);
}

@Test
public void zeroArrivalsProduceZeroArrivalRate() {
    assertEquals(0.0,
            QueueingCalculator.calculateArrivalRate(0, 5), 0.001);
}

@Test(expected = IllegalArgumentException.class)
public void rejectsObservationDurationThatOverflowsArrivalRate() {
    QueueingCalculator.calculateArrivalRate(1, Double.MIN_VALUE);
}

@Test(expected = IllegalArgumentException.class)
public void rejectsServiceTimeAverageThatUnderflowsToZero() {
    QueueingCalculator.calculateAverageServiceTime(
            Double.MIN_VALUE, Integer.MAX_VALUE);
}

@Test(expected = IllegalArgumentException.class)
public void rejectsZeroObservationTime() {
    QueueingCalculator.calculateArrivalRate(20, 0);
}
@Test
public void predictsQueueGrowth() {
    int result = QueueingCalculator.predictQueueLength(10, 3, 2, 5);
    assertEquals(15, result);
}

@Test
public void multipleLaneServiceRateIsCombined() {
    double oneLaneRate =
            QueueingCalculator.calculateServiceRate(30);
    double wait = QueueingCalculator.estimateQueueWaitMinutes(
            12, 3, 30);

    assertEquals(2.0, oneLaneRate, 0.001);
    assertEquals(2.0, wait, 0.001);
}

@Test(expected = IllegalArgumentException.class)
public void rejectsNegativeQueueLengthForWaitEstimate() {
    QueueingCalculator.estimateQueueWaitMinutes(-1, 1, 30);
}

@Test(expected = IllegalArgumentException.class)
public void rejectsUnrepresentableCombinedServiceRate() {
    QueueingCalculator.estimateQueueWaitMinutes(
            Integer.MAX_VALUE, 2, 3.4e-307);
}

@Test(expected = IllegalArgumentException.class)
public void rejectsInvalidArrivalRateForPrediction() {
    QueueingCalculator.predictQueueLength(1, Double.NaN, 1, 1);
}

@Test
public void queueDoesNotBecomeNegative() {
    int result = QueueingCalculator.predictQueueLength(2, 0, 5, 10);
    assertEquals(0, result);
}
 @Test
    public void calculatesAverageServiceTime() {
        double result =
                QueueingCalculator.calculateAverageServiceTime(300, 10);

        assertEquals(30.0, result, 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroVehiclesServed() {
        QueueingCalculator.calculateAverageServiceTime(300, 0);
    }
}