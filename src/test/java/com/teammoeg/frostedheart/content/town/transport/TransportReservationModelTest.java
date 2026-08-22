/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import com.teammoeg.frostedheart.content.town.model.TownModelParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportReservationModelTest {
    private static final double EPSILON = 1.0e-12;
    private static final TransportConsumerParameters PARAMETERS =
            TownModelParameters.currentDefaults().transportConsumers();

    @Test
    void warehouseScaleAndCapacityFollowTheFrozenFormula() {
        assertEquals(0.0, TransportReservationModel.warehouseScaleMetric(0.0));
        assertEquals(1.0, TransportReservationModel.warehouseScaleMetric(1.0));
        assertEquals(8.0, TransportReservationModel.warehouseScaleMetric(64.0));
        assertEquals(Math.sqrt(512.0), TransportReservationModel.warehouseScaleMetric(512.0), EPSILON);

        assertEquals(1.4, TransportReservationModel.warehouseScaleFactor(8.0, PARAMETERS), EPSILON);
        assertEquals(28.0, TransportReservationModel.requiredCapacity(
                TransportEndpointKind.WAREHOUSE_INTERFACE, 20, 8.0, PARAMETERS), EPSILON);
        assertEquals(1280.0 * (1.0 + 0.05 * Math.sqrt(512.0)),
                TransportReservationModel.requiredCapacity(
                        TransportEndpointKind.WAREHOUSE_INTERFACE, 1280, Math.sqrt(512.0), PARAMETERS),
                EPSILON);
        assertEquals(0.0, TransportReservationModel.requiredCapacity(
                TransportEndpointKind.WAREHOUSE_INTERFACE, 0, 8.0, PARAMETERS));
    }

    @Test
    void invalidParametersAndMathProduceInvalidCapacityRatherThanAReservation() {
        assertThrows(IllegalArgumentException.class,
                () -> new TransportConsumerParameters(-1, 1, 1280, 0.05));
        assertThrows(IllegalArgumentException.class,
                () -> new TransportConsumerParameters(20, 0, 1280, 0.05));
        assertThrows(IllegalArgumentException.class,
                () -> new TransportConsumerParameters(20, 2, 1, 0.05));
        assertThrows(IllegalArgumentException.class,
                () -> new TransportConsumerParameters(20, 1, 10, 0.05));
        assertThrows(IllegalArgumentException.class,
                () -> new TransportConsumerParameters(20, 1, 1280, Double.NaN));

        assertTrue(Double.isNaN(TransportReservationModel.warehouseScaleMetric(-1.0)));
        assertTrue(Double.isNaN(TransportReservationModel.warehouseScaleMetric(Double.POSITIVE_INFINITY)));
        assertTrue(Double.isNaN(TransportReservationModel.requiredCapacity(
                TransportEndpointKind.WAREHOUSE_INTERFACE, -1, 1.0, PARAMETERS)));
        assertTrue(Double.isNaN(TransportReservationModel.requiredCapacity(
                TransportEndpointKind.WAREHOUSE_INTERFACE, 1281, 1.0, PARAMETERS)));
        assertTrue(Double.isNaN(TransportReservationModel.requiredCapacity(
                TransportEndpointKind.WAREHOUSE_INTERFACE, 20, -1.0, PARAMETERS)));
        assertTrue(Double.isNaN(TransportReservationModel.requiredCapacity(
                TransportEndpointKind.WAREHOUSE_INTERFACE, 1280, Double.MAX_VALUE, PARAMETERS)));
    }

    @Test
    void capacityComparisonsUseExactlyEightUlpsAsTolerance() {
        double base = 1.0;
        double eightUlpsAbove = advance(base, 8);
        double nineUlpsAbove = advance(base, 9);

        assertTrue(TransportReservationModel.lessThanOrNearlyEqual(eightUlpsAbove, base));
        assertFalse(TransportReservationModel.meaningfullyGreater(eightUlpsAbove, base));
        assertFalse(TransportReservationModel.lessThanOrNearlyEqual(nineUlpsAbove, base));
        assertTrue(TransportReservationModel.meaningfullyGreater(nineUlpsAbove, base));
        assertFalse(TransportReservationModel.lessThanOrNearlyEqual(Double.NaN, base));
    }

    @Test
    void shortageScaleAndAdmissionUseDerivedCapacityWithoutQuantization() {
        assertEquals(1.0, TransportReservationModel.effectiveRateScale(28.0, 28.0));
        assertEquals(0.5, TransportReservationModel.effectiveRateScale(14.0, 28.0), EPSILON);
        assertEquals(1.0, TransportReservationModel.effectiveRateScale(0.0, 0.0));
        assertTrue(Double.isNaN(TransportReservationModel.effectiveRateScale(-1.0, 0.0)));

        TransportReservationModel.AdmissionEvaluation unchanged =
                TransportReservationModel.evaluateAdmission(20.0, 28.0, 28.0, 20.0);
        assertTrue(unchanged.valid());
        assertTrue(unchanged.accepted());
        assertTrue(unchanged.doesNotIncreaseEndpoint());
        assertEquals(20.0, unchanged.candidateTownReservedCapacity());

        TransportReservationModel.AdmissionEvaluation accepted =
                TransportReservationModel.evaluateAdmission(30.0, 28.0, 28.0, 29.0);
        assertTrue(accepted.accepted());
        assertEquals(1.0, accepted.requiredAdditionalCapacity());

        TransportReservationModel.AdmissionEvaluation rejected =
                TransportReservationModel.evaluateAdmission(30.0, 28.0, 28.0, 31.0);
        assertFalse(rejected.accepted());
        assertEquals(3.0, rejected.requiredAdditionalCapacity());
    }

    private static double advance(double value, int ulps) {
        double result = value;
        for (int index = 0; index < ulps; index++) {
            result = Math.nextUp(result);
        }
        return result;
    }
}
