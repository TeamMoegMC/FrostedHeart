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

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WearableThermalProfileTest {
    private static final double EPSILON = 1.0e-12D;

    @Test
    void recordKeepsOnlyTheFourAuthoritativeParameters() {
        assertArrayEquals(
                new String[]{
                        "capacityRatio",
                        "surfaceCapacityFraction",
                        "coreSurfaceTransferRatePerSecond",
                        "playerTransferRatePerSecond"
                },
                Arrays.stream(WearableThermalProfile.class.getRecordComponents())
                        .map(component -> component.getName())
                        .toArray(String[]::new)
        );
    }

    @Test
    void warmStoneDefaultsAndDerivedRatesAreFrozen() {
        WearableThermalProfile profile = WearableThermalProfile.WARM_STONE_DEFAULT;

        assertEquals(0.10D, profile.capacityRatio());
        assertEquals(0.20D, profile.surfaceCapacityFraction());
        assertEquals(6.1613e-5D, profile.coreSurfaceTransferRatePerSecond());
        assertEquals(1.2e-4D, profile.playerTransferRatePerSecond());
        assertEquals(0.08D, profile.coreCapacityRatio(), EPSILON);
        assertEquals(0.02D, profile.surfaceCapacityRatio(), EPSILON);
        assertEquals(6.0e-5D,
                profile.inventoryEnvironmentTransferRatePerSecond(), EPSILON);
        assertEquals(9.6e-4D,
                profile.droppedEnvironmentTransferRatePerSecond(), EPSILON);
    }

    @Test
    void hotWaterBagDefaultsAndDerivedRatesAreFrozen() {
        WearableThermalProfile profile = WearableThermalProfile.HOT_WATER_BAG_DEFAULT;

        assertEquals(0.25D, profile.capacityRatio());
        assertEquals(0.20D, profile.surfaceCapacityFraction());
        assertEquals(9.2420e-4D, profile.coreSurfaceTransferRatePerSecond());
        assertEquals(8.0e-5D, profile.playerTransferRatePerSecond());
        assertEquals(0.20D, profile.coreCapacityRatio(), EPSILON);
        assertEquals(0.05D, profile.surfaceCapacityRatio(), EPSILON);
        assertEquals(4.0e-5D,
                profile.inventoryEnvironmentTransferRatePerSecond(), EPSILON);
        assertEquals(6.4e-4D,
                profile.droppedEnvironmentTransferRatePerSecond(), EPSILON);
    }

    @Test
    void invalidSourceProfilesAreRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class,
                () -> new WearableThermalProfile(0.0D, 0.2D, 1.0D, 1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new WearableThermalProfile(0.1D, 1.0D, 1.0D, 1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new WearableThermalProfile(0.1D, 0.2D, Double.NaN, 1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new WearableThermalProfile(0.1D, 0.2D, 1.0D, -1.0D));
    }
}
