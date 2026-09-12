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
            warm_stone,worn,0,60,56.811278,42.605773,37.142982
            warm_stone,worn,0,300,49.026611,41.203807,37.793795
            warm_stone,worn,0,900,41.234803,39.299048,38.455235
            warm_stone,worn,0,1800,38.988101,38.749823,38.645955
            warm_stone,inventory,0,0,60.000000,60.000000,0.000000
            warm_stone,inventory,0,60,57.079352,32.900500,0.000000
            warm_stone,inventory,0,300,39.263378,18.817806,0.000000
            warm_stone,inventory,0,900,14.992894,7.183550,0.000000
            warm_stone,inventory,0,1800,3.537706,1.695022,0.000000
            warm_stone,dropped,100,0,0.000000,0.000000,13.333333
            warm_stone,dropped,100,60,2.017045,12.775638,13.333333
            warm_stone,dropped,100,300,7.728699,13.057125,13.333333
            warm_stone,dropped,100,900,12.365823,13.285652,13.333333
            warm_stone,dropped,100,1800,13.263940,13.329913,13.333333
            hot_water_bag,worn,0,0,60.000000,37.000000,37.000000
            hot_water_bag,worn,0,60,54.379629,52.940131,37.327068
            hot_water_bag,worn,0,300,49.423448,48.540183,38.538301
            hot_water_bag,worn,0,900,43.530731,43.242749,39.981716
            hot_water_bag,worn,0,1800,41.210721,41.157108,40.550000
            hot_water_bag,inventory,0,0,60.000000,60.000000,0.000000
            hot_water_bag,inventory,0,60,57.746241,55.337622,0.000000
            hot_water_bag,inventory,0,300,47.969622,45.962076,0.000000
            hot_water_bag,inventory,0,900,30.167765,28.905233,0.000000
            hot_water_bag,inventory,0,1800,15.045561,14.415899,0.000000
            hot_water_bag,dropped,100,0,0.000000,0.000000,13.333333
            hot_water_bag,dropped,100,60,4.596773,8.365484,13.333333
            hot_water_bag,dropped,100,300,12.041818,12.599057,13.333333
            hot_water_bag,dropped,100,900,13.322482,13.327164,13.333333
            hot_water_bag,dropped,100,1800,13.333325,13.333329,13.333333
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
