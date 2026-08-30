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

import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalExchangeKernel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreeNodeWearableHeatExchangeTest {
    private static final double EPSILON = 1.0e-12D;

    @Test
    void frozenHalfLivesRoundTripToAuthoritativeTransferRates() {
        assertHalfLife(
                WearableThermalProfile.WARM_STONE_DEFAULT,
                180.0D,
                6.1613e-5D,
                3.0e-10D
        );
        assertHalfLife(
                WearableThermalProfile.HOT_WATER_BAG_DEFAULT,
                30.0D,
                9.2420e-4D,
                2.0e-10D
        );
    }

    @Test
    void playerInitialRateUsesTheSurfaceDifferenceDirectly() {
        assertEquals(
                0.0024D,
                ThreeNodeWearableHeatExchange.playerTemperatureRatePerSecond(
                        WearableThermalProfile.WARM_STONE_DEFAULT,
                        60.0D,
                        40.0D
                ),
                EPSILON
        );
        assertEquals(
                -0.0024D,
                ThreeNodeWearableHeatExchange.playerTemperatureRatePerSecond(
                        WearableThermalProfile.WARM_STONE_DEFAULT,
                        20.0D,
                        40.0D
                ),
                EPSILON
        );
        assertEquals(
                0.0016D,
                ThreeNodeWearableHeatExchange.playerTemperatureRatePerSecond(
                        WearableThermalProfile.HOT_WATER_BAG_DEFAULT,
                        60.0D,
                        40.0D
                ),
                EPSILON
        );
    }

    @Test
    void randomizedExchangesConserveNormalizedEnthalpyAndStayInBounds() {
        SplittableRandom random = new SplittableRandom(0x5741524D53544F4EL);
        ThreeNodeWearableHeatExchange.MutableResult result =
                new ThreeNodeWearableHeatExchange.MutableResult();
        ThreeNodeWearableHeatExchange.Scratch scratch =
                new ThreeNodeWearableHeatExchange.Scratch();

        for (int iteration = 0; iteration < 2_000; iteration++) {
            WearableThermalProfile profile = iteration % 2 == 0
                    ? WearableThermalProfile.WARM_STONE_DEFAULT
                    : WearableThermalProfile.HOT_WATER_BAG_DEFAULT;
            double core = random.nextDouble(-100.0D, 200.0D);
            double surface = random.nextDouble(-100.0D, 200.0D);
            double player = random.nextDouble(-100.0D, 200.0D);
            double dtSeconds = random.nextDouble(0.0D, 300.0D);
            double initialEnthalpy = normalizedEnthalpy(profile, core, surface, player);
            double lowerBound = Math.min(core, Math.min(surface, player));
            double upperBound = Math.max(core, Math.max(surface, player));

            ThreeNodeWearableHeatExchange.exchangeInto(
                    profile, core, surface, player, dtSeconds, result, scratch);

            assertEquals(ThermalExchangeKernel.Status.APPLIED, result.status());
            double scale = Math.max(1.0D, Math.abs(initialEnthalpy));
            assertEquals(
                    initialEnthalpy,
                    normalizedEnthalpy(
                            profile,
                            result.reservoirCoreTemperatureC(),
                            result.reservoirSurfaceTemperatureC(),
                            result.playerTemperatureC()
                    ),
                    scale * 2.0e-12D
            );
            assertWithin(result.reservoirCoreTemperatureC(), lowerBound, upperBound);
            assertWithin(result.reservoirSurfaceTemperatureC(), lowerBound, upperBound);
            assertWithin(result.playerTemperatureC(), lowerBound, upperBound);
        }
    }

    @Test
    void equalTemperaturesRemainExactlyUnchanged() {
        ThreeNodeWearableHeatExchange.MutableResult result = exchange(
                WearableThermalProfile.WARM_STONE_DEFAULT,
                37.0D,
                37.0D,
                37.0D,
                10_000.0D
        );

        assertEquals(37.0D, result.reservoirCoreTemperatureC());
        assertEquals(37.0D, result.reservoirSurfaceTemperatureC());
        assertEquals(37.0D, result.playerTemperatureC());
    }

    @Test
    void longExchangeApproachesCapacityWeightedEquilibrium() {
        assertEquilibrium(WearableThermalProfile.WARM_STONE_DEFAULT);
        assertEquilibrium(WearableThermalProfile.HOT_WATER_BAG_DEFAULT);
    }

    @Test
    void hotCoreWithPlayerMatchedSurfaceDoesNotCollapseToOneItemTemperature() {
        ThreeNodeWearableHeatExchange.MutableResult result = exchange(
                WearableThermalProfile.WARM_STONE_DEFAULT,
                60.0D,
                37.0D,
                37.0D,
                10.0D
        );

        assertTrue(result.reservoirCoreTemperatureC() > result.reservoirSurfaceTemperatureC());
        assertTrue(result.reservoirSurfaceTemperatureC() > result.playerTemperatureC());
        assertTrue(result.playerTemperatureC() > 37.0D);
    }

    @Test
    void elapsedTimeSplittingStaysWithinFrozenIntegratorTolerance() {
        WearableThermalProfile[] profiles = {
                WearableThermalProfile.WARM_STONE_DEFAULT,
                WearableThermalProfile.HOT_WATER_BAG_DEFAULT
        };
        for (WearableThermalProfile profile : profiles) {
            ThreeNodeWearableHeatExchange.MutableResult oneCall = exchange(
                    profile, 60.0D, 5.0D, 37.0D, 180.0D);
            ThreeNodeWearableHeatExchange.MutableResult split = repeat(
                    profile, 60.0D, 5.0D, 37.0D, 0.25D, 720);

            assertEquals(
                    oneCall.reservoirCoreTemperatureC(),
                    split.reservoirCoreTemperatureC(),
                    1.0e-4D
            );
            assertEquals(
                    oneCall.reservoirSurfaceTemperatureC(),
                    split.reservoirSurfaceTemperatureC(),
                    1.0e-4D
            );
            assertEquals(
                    oneCall.playerTemperatureC(),
                    split.playerTemperatureC(),
                    1.0e-4D
            );
        }
    }

    @Test
    void invalidInputsAreObservableAtomicNoOps() {
        ThreeNodeWearableHeatExchange.MutableResult result =
                new ThreeNodeWearableHeatExchange.MutableResult();
        ThreeNodeWearableHeatExchange.Scratch scratch =
                new ThreeNodeWearableHeatExchange.Scratch();

        ThreeNodeWearableHeatExchange.exchangeInto(
                WearableThermalProfile.WARM_STONE_DEFAULT,
                60.0D,
                40.0D,
                37.0D,
                Double.NaN,
                result,
                scratch
        );

        assertFalse(result.applied());
        assertEquals(60.0D, result.reservoirCoreTemperatureC());
        assertEquals(40.0D, result.reservoirSurfaceTemperatureC());
        assertEquals(37.0D, result.playerTemperatureC());
    }

    @Test
    void reusableCarriersContainNoArraysOrCollectionState() {
        assertScalarCarrier(ThreeNodeWearableHeatExchange.MutableResult.class);
        assertScalarCarrier(ThreeNodeWearableHeatExchange.Scratch.class);

        ThreeNodeWearableHeatExchange.MutableResult result =
                new ThreeNodeWearableHeatExchange.MutableResult();
        ThreeNodeWearableHeatExchange.Scratch scratch =
                new ThreeNodeWearableHeatExchange.Scratch();
        ThreeNodeWearableHeatExchange.MutableResult returned = result;
        ThreeNodeWearableHeatExchange.exchangeInto(
                WearableThermalProfile.WARM_STONE_DEFAULT,
                60.0D,
                60.0D,
                37.0D,
                1.0D,
                returned,
                scratch
        );

        assertSame(result, returned);
    }

    private static void assertHalfLife(
            WearableThermalProfile profile,
            double expectedHalfLife,
            double frozenRate,
            double halfLifeTolerance
    ) {
        double calculatedRate =
                ThreeNodeWearableHeatExchange
                        .coreSurfaceTransferRatePerSecondFromHalfLife(
                                profile.capacityRatio(),
                                profile.surfaceCapacityFraction(),
                                expectedHalfLife
                        );
        // Profiles retain the plan's rounded source constants, while the helper
        // evaluates the full logarithmic formula.
        assertEquals(frozenRate, calculatedRate, 5.0e-9D);
        assertEquals(
                expectedHalfLife,
                ThreeNodeWearableHeatExchange.coreSurfaceHalfLifeSeconds(
                        profile.capacityRatio(),
                        profile.surfaceCapacityFraction(),
                        calculatedRate
                ),
                halfLifeTolerance
        );
        assertEquals(
                expectedHalfLife,
                ThreeNodeWearableHeatExchange.coreSurfaceHalfLifeSeconds(
                        profile.capacityRatio(),
                        profile.surfaceCapacityFraction(),
                        profile.coreSurfaceTransferRatePerSecond()
                ),
                5.0e-4D
        );
    }

    private static void assertEquilibrium(WearableThermalProfile profile) {
        double core = 60.0D;
        double surface = 20.0D;
        double player = 37.0D;
        double expected = normalizedEnthalpy(profile, core, surface, player)
                / (profile.capacityRatio() + 1.0D);
        ThreeNodeWearableHeatExchange.MutableResult result = exchange(
                profile, core, surface, player, 100_000.0D);

        assertEquals(expected, result.reservoirCoreTemperatureC(), 2.0e-3D);
        assertEquals(expected, result.reservoirSurfaceTemperatureC(), 2.0e-3D);
        assertEquals(expected, result.playerTemperatureC(), 2.0e-3D);
    }

    private static ThreeNodeWearableHeatExchange.MutableResult repeat(
            WearableThermalProfile profile,
            double core,
            double surface,
            double player,
            double dtSeconds,
            int count
    ) {
        ThreeNodeWearableHeatExchange.MutableResult result =
                new ThreeNodeWearableHeatExchange.MutableResult();
        ThreeNodeWearableHeatExchange.Scratch scratch =
                new ThreeNodeWearableHeatExchange.Scratch();
        for (int step = 0; step < count; step++) {
            ThreeNodeWearableHeatExchange.exchangeInto(
                    profile, core, surface, player, dtSeconds, result, scratch);
            core = result.reservoirCoreTemperatureC();
            surface = result.reservoirSurfaceTemperatureC();
            player = result.playerTemperatureC();
        }
        return result;
    }

    private static ThreeNodeWearableHeatExchange.MutableResult exchange(
            WearableThermalProfile profile,
            double core,
            double surface,
            double player,
            double dtSeconds
    ) {
        ThreeNodeWearableHeatExchange.MutableResult result =
                new ThreeNodeWearableHeatExchange.MutableResult();
        ThreeNodeWearableHeatExchange.exchangeInto(
                profile,
                core,
                surface,
                player,
                dtSeconds,
                result,
                new ThreeNodeWearableHeatExchange.Scratch()
        );
        return result;
    }

    private static double normalizedEnthalpy(
            WearableThermalProfile profile,
            double core,
            double surface,
            double player
    ) {
        return profile.coreCapacityRatio() * core
                + profile.surfaceCapacityRatio() * surface
                + player;
    }

    private static void assertWithin(double value, double lower, double upper) {
        assertTrue(value >= lower - EPSILON, value + " below " + lower);
        assertTrue(value <= upper + EPSILON, value + " above " + upper);
    }

    private static void assertScalarCarrier(Class<?> carrierClass) {
        for (Field field : carrierClass.getDeclaredFields()) {
            assertFalse(field.getType().isArray(), field.toString());
            assertFalse(Collection.class.isAssignableFrom(field.getType()), field.toString());
            assertFalse(Map.class.isAssignableFrom(field.getType()), field.toString());
        }
    }
}
