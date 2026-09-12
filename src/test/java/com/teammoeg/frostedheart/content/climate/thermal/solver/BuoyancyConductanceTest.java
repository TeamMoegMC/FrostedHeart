/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.solver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuoyancyConductanceTest {
    private static final BuoyancyConductance.Parameters PARAMETERS =
            new BuoyancyConductance.Parameters(0.25D, 4.0D, 20.0D);

    @Test
    void hotBelowIncreasesAndColdBelowReducesConductance() {
        BuoyancyConductance.MutableResult unstable = evaluate(
                10.0D, 40.0D, 0.0D, 0.0D, 4.0D, PARAMETERS);
        BuoyancyConductance.MutableResult stable = evaluate(
                10.0D, 0.0D, 0.0D, 40.0D, 4.0D, PARAMETERS);

        assertEquals(30.0D, unstable.conductanceWPerK());
        assertEquals(2.5D, stable.conductanceWPerK());
    }

    @Test
    void endpointSwapLeavesPhysicalFactorExactlyUnchanged() {
        BuoyancyConductance.MutableResult first = evaluate(
                7.5D, 35.0D, -8.0D, -5.0D, 12.0D, PARAMETERS);
        BuoyancyConductance.MutableResult swapped = evaluate(
                7.5D, -5.0D, 12.0D, 35.0D, -8.0D, PARAMETERS);

        assertEquals(first.applied(), swapped.applied());
        assertEquals(first.conductanceWPerK(), swapped.conductanceWPerK());
    }

    @Test
    void horizontalPairUsesNeutralClampedFactorAndClampNeverGoesNegative() {
        BuoyancyConductance.MutableResult horizontal = evaluate(
                8.0D, 1_000.0D, 2.0D, -1_000.0D, 2.0D, PARAMETERS);
        BuoyancyConductance.MutableResult stronglyStable = evaluate(
                8.0D, -1_000.0D, 0.0D, 1_000.0D, 1.0D, PARAMETERS);

        assertEquals(8.0D, horizontal.conductanceWPerK());
        assertEquals(8.0D * PARAMETERS.minimumFactor(),
                stronglyStable.conductanceWPerK());
        assertTrue(stronglyStable.conductanceWPerK() >= 0.0D);
    }

    @Test
    void invalidRuntimeValuesDegradeAndInvalidProfilesAreRejected() {
        BuoyancyConductance.MutableResult result = evaluate(
                10.0D, Double.NaN, 0.0D, 5.0D, 1.0D, PARAMETERS);

        assertFalse(result.applied());
        assertThrows(IllegalArgumentException.class,
                () -> new BuoyancyConductance.Parameters(-0.1D, 2.0D, 10.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new BuoyancyConductance.Parameters(2.0D, 1.0D, 10.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new BuoyancyConductance.Parameters(0.0D, 1.0D, 0.0D));
    }

    private static BuoyancyConductance.MutableResult evaluate(
            double baseConductanceWPerK,
            double temperatureAC,
            double centerYA,
            double temperatureBC,
            double centerYB,
            BuoyancyConductance.Parameters parameters
    ) {
        BuoyancyConductance.MutableResult result =
                new BuoyancyConductance.MutableResult();
        BuoyancyConductance.evaluateInto(
                baseConductanceWPerK,
                temperatureAC,
                centerYA,
                temperatureBC,
                centerYB,
                parameters,
                result);
        return result;
    }
}
