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

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WearableThermalCurveFixtureTest {
    private static final int[] CHECKPOINT_SECONDS = {0, 60, 300, 900, 1_800};
    private static final String EXPECTED_CURVES = """
            profile,mode,q_w_per_m2,time_s,core_c,surface_c,player_or_environment_c
            warm_stone,worn,0,0,60.000000,37.000000,37.000000
            warm_stone,worn,0,60,59.041395,40.202377,37.012641
            warm_stone,worn,0,300,56.262456,43.477195,37.169460
            warm_stone,worn,0,900,51.372983,42.484287,37.580476
            warm_stone,worn,0,1800,46.491909,41.020417,38.000239
            warm_stone,inventory,0,0,60.000000,60.000000,0.000000
            warm_stone,inventory,0,60,59.781563,50.938170,0.000000
            warm_stone,inventory,0,300,56.556008,34.382145,0.000000
            warm_stone,inventory,0,900,46.076205,24.874303,0.000000
            warm_stone,inventory,0,1800,33.460260,18.005584,0.000000
            warm_stone,dropped,100,0,0.000000,0.000000,13.333333
            warm_stone,dropped,100,60,0.391836,11.959269,13.333333
            warm_stone,dropped,100,300,2.446031,12.667365,13.333333
            warm_stone,dropped,100,900,6.278068,12.901770,13.333333
            warm_stone,dropped,100,1800,9.652867,13.108203,13.333333
            hot_water_bag,worn,0,0,60.000000,37.000000,37.000000
            hot_water_bag,worn,0,60,56.492004,50.244274,37.039385
            hot_water_bag,worn,0,300,54.251439,53.125186,37.343453
            hot_water_bag,worn,0,900,51.491000,50.605103,38.021545
            hot_water_bag,worn,0,1800,48.368763,47.738715,38.789312
            hot_water_bag,inventory,0,0,60.000000,60.000000,0.000000
            hot_water_bag,inventory,0,60,59.739210,58.212931,0.000000
            hot_water_bag,inventory,0,300,57.640041,55.699968,0.000000
            hot_water_bag,inventory,0,900,52.500762,50.732188,0.000000
            hot_water_bag,inventory,0,1800,45.637727,44.100345,0.000000
            hot_water_bag,dropped,100,0,0.000000,0.000000,13.333333
            hot_water_bag,dropped,100,60,0.765510,4.830392,13.333333
            hot_water_bag,dropped,100,300,4.977087,8.106908,13.333333
            hot_water_bag,dropped,100,900,10.375219,11.483239,13.333333
            hot_water_bag,dropped,100,1800,12.710289,12.943663,13.333333
            """;

    @Test
    void frozenProfilesProduceReproducibleSyntheticCurves() {
        String curves = curvesCsv();
        System.out.print(curves);

        assertEquals(EXPECTED_CURVES, curves);
    }

    private static String curvesCsv() {
        StringBuilder output = new StringBuilder();
        output.append("profile,mode,q_w_per_m2,time_s,core_c,surface_c,player_or_environment_c\n");
        appendProfileCurves(output, "warm_stone", WearableThermalProfile.WARM_STONE_DEFAULT);
        appendProfileCurves(output, "hot_water_bag", WearableThermalProfile.HOT_WATER_BAG_DEFAULT);
        return output.toString();
    }

    private static void appendProfileCurves(
            StringBuilder output,
            String profileName,
            WearableThermalProfile profile
    ) {
        appendWearableCurve(output, profileName, profile);
        appendEnvironmentCurve(output, profileName, profile, false);
        appendEnvironmentCurve(output, profileName, profile, true);
    }

    private static void appendWearableCurve(
            StringBuilder output,
            String profileName,
            WearableThermalProfile profile
    ) {
        ThreeNodeWearableHeatExchange.Scratch scratch =
                new ThreeNodeWearableHeatExchange.Scratch();
        ThreeNodeWearableHeatExchange.MutableResult result =
                new ThreeNodeWearableHeatExchange.MutableResult();
        double core = 60.0D;
        double surface = 37.0D;
        double player = 37.0D;
        int previousTime = 0;

        for (int checkpoint : CHECKPOINT_SECONDS) {
            ThreeNodeWearableHeatExchange.exchangeInto(
                    profile,
                    core,
                    surface,
                    player,
                    checkpoint - previousTime,
                    result,
                    scratch
            );
            core = result.reservoirCoreTemperatureC();
            surface = result.reservoirSurfaceTemperatureC();
            player = result.playerTemperatureC();
            appendRow(output, profileName, "worn", 0.0D, checkpoint,
                    core, surface, player);
            previousTime = checkpoint;
        }
    }

    private static void appendEnvironmentCurve(
            StringBuilder output,
            String profileName,
            WearableThermalProfile profile,
            boolean dropped
    ) {
        ReservoirEnvironmentExchange.Scratch scratch =
                new ReservoirEnvironmentExchange.Scratch();
        ReservoirEnvironmentExchange.MutableResult result =
                new ReservoirEnvironmentExchange.MutableResult();
        double core = dropped ? 0.0D : 60.0D;
        double surface = core;
        double air = 0.0D;
        double radiantFlux = dropped ? 100.0D : 0.0D;
        int previousTime = 0;

        for (int checkpoint : CHECKPOINT_SECONDS) {
            if (dropped) {
                ReservoirEnvironmentExchange.advanceDroppedInto(
                        profile,
                        core,
                        surface,
                        air,
                        radiantFlux,
                        checkpoint - previousTime,
                        scratch,
                        result
                );
            } else {
                ReservoirEnvironmentExchange.advanceInventoryInto(
                        profile,
                        core,
                        surface,
                        air,
                        checkpoint - previousTime,
                        scratch,
                        result
                );
            }
            core = result.coreTemperatureC();
            surface = result.surfaceTemperatureC();
            appendRow(output, profileName, dropped ? "dropped" : "inventory",
                    radiantFlux, checkpoint, core, surface,
                    result.effectiveEnvironmentTemperatureC());
            previousTime = checkpoint;
        }
    }

    private static void appendRow(
            StringBuilder output,
            String profileName,
            String mode,
            double radiantFlux,
            int timeSeconds,
            double coreTemperatureC,
            double surfaceTemperatureC,
            double targetTemperatureC
    ) {
        output.append(String.format(
                Locale.ROOT,
                "%s,%s,%.0f,%d,%.6f,%.6f,%.6f",
                profileName,
                mode,
                radiantFlux,
                timeSeconds,
                coreTemperatureC,
                surfaceTemperatureC,
                targetTemperatureC
        )).append('\n');
    }
}
