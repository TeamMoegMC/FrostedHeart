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
        BuoyancyConductance.Result unstable = BuoyancyConductance.evaluate(
                10.0D, 40.0D, 0.0D, 0.0D, 4.0D, PARAMETERS);
        BuoyancyConductance.Result stable = BuoyancyConductance.evaluate(
                10.0D, 0.0D, 0.0D, 40.0D, 4.0D, PARAMETERS);

        assertTrue(unstable.factor() > 1.0D);
        assertTrue(stable.factor() < 1.0D);
        assertEquals(30.0D, unstable.conductanceWPerK());
        assertEquals(2.5D, stable.conductanceWPerK());
    }

    @Test
    void endpointSwapLeavesPhysicalFactorExactlyUnchanged() {
        BuoyancyConductance.Result first = BuoyancyConductance.evaluate(
                7.5D, 35.0D, -8.0D, -5.0D, 12.0D, PARAMETERS);
        BuoyancyConductance.Result swapped = BuoyancyConductance.evaluate(
                7.5D, -5.0D, 12.0D, 35.0D, -8.0D, PARAMETERS);

        assertEquals(first.status(), swapped.status());
        assertEquals(first.factor(), swapped.factor());
        assertEquals(first.conductanceWPerK(), swapped.conductanceWPerK());
    }

    @Test
    void horizontalPairUsesNeutralClampedFactorAndClampNeverGoesNegative() {
        BuoyancyConductance.Result horizontal = BuoyancyConductance.evaluate(
                8.0D, 1_000.0D, 2.0D, -1_000.0D, 2.0D, PARAMETERS);
        BuoyancyConductance.Result stronglyStable = BuoyancyConductance.evaluate(
                8.0D, -1_000.0D, 0.0D, 1_000.0D, 1.0D, PARAMETERS);

        assertEquals(1.0D, horizontal.factor());
        assertEquals(PARAMETERS.minimumFactor(), stronglyStable.factor());
        assertTrue(stronglyStable.conductanceWPerK() >= 0.0D);
    }

    @Test
    void invalidRuntimeValuesDegradeAndInvalidProfilesAreRejected() {
        BuoyancyConductance.Result result = BuoyancyConductance.evaluate(
                10.0D, Double.NaN, 0.0D, 5.0D, 1.0D, PARAMETERS);

        assertEquals(BuoyancyConductance.Status.NUMERIC_DEGRADED, result.status());
        assertFalse(result.applied());
        assertThrows(IllegalArgumentException.class,
                () -> new BuoyancyConductance.Parameters(-0.1D, 2.0D, 10.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new BuoyancyConductance.Parameters(2.0D, 1.0D, 10.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new BuoyancyConductance.Parameters(0.0D, 1.0D, 0.0D));
    }
}
