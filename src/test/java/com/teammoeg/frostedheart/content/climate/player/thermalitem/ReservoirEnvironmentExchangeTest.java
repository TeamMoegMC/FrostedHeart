/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.player.thermalitem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservoirEnvironmentExchangeTest {
    private static final double EPSILON = 1.0e-11D;

    @Test
    void inventoryUsesAirOnlyWhileDroppedUsesTheSharedRadiantBoundary() {
        ReservoirEnvironmentExchange.MutableResult inventory = inventory(
                WearableThermalProfile.WARM_STONE_DEFAULT,
                20.0D, 20.0D, 20.0D, 100.0D, 20.0D);
        ReservoirEnvironmentExchange.MutableResult droppedWithoutRadiation = dropped(
                WearableThermalProfile.WARM_STONE_DEFAULT,
                20.0D, 20.0D, 20.0D, 0.0D, 20.0D);
        ReservoirEnvironmentExchange.MutableResult droppedWithRadiation = dropped(
                WearableThermalProfile.WARM_STONE_DEFAULT,
                20.0D, 20.0D, 20.0D, 100.0D, 20.0D);

        assertTrue(inventory.applied());
        assertEquals(20.0D, inventory.effectiveEnvironmentTemperatureC(), EPSILON);
        assertEquals(20.0D, inventory.coreTemperatureC(), EPSILON);
        assertEquals(20.0D, inventory.surfaceTemperatureC(), EPSILON);
        assertEquals(droppedWithoutRadiation.coreTemperatureC(), inventory.coreTemperatureC(), EPSILON);
        assertEquals(droppedWithoutRadiation.surfaceTemperatureC(), inventory.surfaceTemperatureC(), EPSILON);
        assertEquals(33.333333333333336D,
                droppedWithRadiation.effectiveEnvironmentTemperatureC(), EPSILON);
        assertTrue(droppedWithRadiation.surfaceTemperatureC()
                > droppedWithRadiation.coreTemperatureC());
        assertTrue(droppedWithRadiation.coreTemperatureC() > 20.0D);
    }

    @Test
    void derivedInventoryAndDroppedConductanceCreateDistinctResponses() {
        ReservoirEnvironmentExchange.MutableResult inventory = inventory(
                WearableThermalProfile.WARM_STONE_DEFAULT,
                0.0D, 0.0D, 40.0D, 0.0D, 20.0D);
        ReservoirEnvironmentExchange.MutableResult dropped = dropped(
                WearableThermalProfile.WARM_STONE_DEFAULT,
                0.0D, 0.0D, 40.0D, 0.0D, 20.0D);

        assertEquals(0.5D * WearableThermalProfile.WARM_STONE_DEFAULT
                        .playerTransferRatePerSecond(),
                WearableThermalProfile.WARM_STONE_DEFAULT
                        .inventoryEnvironmentTransferRatePerSecond(), EPSILON);
        assertEquals(8.0D * WearableThermalProfile.WARM_STONE_DEFAULT
                        .playerTransferRatePerSecond(),
                WearableThermalProfile.WARM_STONE_DEFAULT
                        .droppedEnvironmentTransferRatePerSecond(), EPSILON);
        assertTrue(dropped.surfaceTemperatureC() > inventory.surfaceTemperatureC());
        assertTrue(inventory.surfaceTemperatureC() > inventory.coreTemperatureC());
    }

    @Test
    void coreSurfaceOnlyExchangeUsesProfileCapacityAndHalfLife() {
        WearableThermalProfile warmStone = WearableThermalProfile.WARM_STONE_DEFAULT;
        ReservoirEnvironmentExchange.MutableResult result = explicit(
                warmStone, 100.0D, 0.0D, 0.0D, 0.0D, 180.0D);

        double initialDifference = 100.0D;
        double expectedDifference = initialDifference * Math.exp(
                -warmStone.coreSurfaceTransferRatePerSecond()
                        * (1.0D / warmStone.coreCapacityRatio()
                        + 1.0D / warmStone.surfaceCapacityRatio())
                        * 180.0D);
        double equilibriumTemperature = 80.0D;
        assertEquals(expectedDifference,
                result.coreTemperatureC() - result.surfaceTemperatureC(), EPSILON);
        assertEquals(equilibriumTemperature
                        + warmStone.surfaceCapacityRatio()
                        / warmStone.capacityRatio() * expectedDifference,
                result.coreTemperatureC(), EPSILON);
        assertEquals(equilibriumTemperature
                        - warmStone.coreCapacityRatio()
                        / warmStone.capacityRatio() * expectedDifference,
                result.surfaceTemperatureC(), EPSILON);
    }

    @Test
    void fixedSubstepsMakeACombinedIntervalMatchRepeatedIntervals() {
        WearableThermalProfile profile = WearableThermalProfile.HOT_WATER_BAG_DEFAULT;
        ReservoirEnvironmentExchange.MutableResult combined = dropped(
                profile, 90.0D, 15.0D, -10.0D, 100.0D, 120.0D);

        double core = 90.0D;
        double surface = 15.0D;
        for (int second = 0; second < 120; second++) {
            ReservoirEnvironmentExchange.MutableResult step = dropped(
                    profile, core, surface, -10.0D, 100.0D, 1.0D);
            core = step.coreTemperatureC();
            surface = step.surfaceTemperatureC();
        }

        assertEquals(core, combined.coreTemperatureC(), EPSILON);
        assertEquals(surface, combined.surfaceTemperatureC(), EPSILON);
    }

    @Test
    void invalidValuesDegradeToFiniteBoundedResults() {
        ReservoirEnvironmentExchange.MutableResult result = explicit(
                WearableThermalProfile.WARM_STONE_DEFAULT,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NaN,
                1.0D,
                Double.NaN);

        assertFalse(result.applied());
        assertEquals(ReservoirEnvironmentExchange.Status.NUMERIC_DEGRADED, result.status());
        assertTrue(Double.isFinite(result.coreTemperatureC()));
        assertTrue(Double.isFinite(result.surfaceTemperatureC()));
        assertTrue(Double.isFinite(result.effectiveEnvironmentTemperatureC()));
        assertTrue(result.coreTemperatureC() >= ReservoirEnvironmentExchange.MINIMUM_TEMPERATURE_C);
        assertTrue(result.surfaceTemperatureC() <= ReservoirEnvironmentExchange.MAXIMUM_TEMPERATURE_C);
    }

    private static ReservoirEnvironmentExchange.MutableResult inventory(
            WearableThermalProfile profile,
            double core,
            double surface,
            double air,
            double ignoredRadiantFlux,
            double seconds
    ) {
        ReservoirEnvironmentExchange.MutableResult result =
                new ReservoirEnvironmentExchange.MutableResult();
        ReservoirEnvironmentExchange.advanceInventoryInto(
                profile, core, surface, air, seconds,
                new ReservoirEnvironmentExchange.Scratch(), result);
        return result;
    }

    private static ReservoirEnvironmentExchange.MutableResult dropped(
            WearableThermalProfile profile,
            double core,
            double surface,
            double air,
            double radiantFlux,
            double seconds
    ) {
        ReservoirEnvironmentExchange.MutableResult result =
                new ReservoirEnvironmentExchange.MutableResult();
        ReservoirEnvironmentExchange.advanceDroppedInto(
                profile, core, surface, air, radiantFlux, seconds,
                new ReservoirEnvironmentExchange.Scratch(), result);
        return result;
    }

    private static ReservoirEnvironmentExchange.MutableResult explicit(
            WearableThermalProfile profile,
            double core,
            double surface,
            double environment,
            double conductance,
            double seconds
    ) {
        ReservoirEnvironmentExchange.MutableResult result =
                new ReservoirEnvironmentExchange.MutableResult();
        ReservoirEnvironmentExchange.advanceInto(
                profile, core, surface, environment, conductance, seconds,
                new ReservoirEnvironmentExchange.Scratch(), result);
        return result;
    }
}
