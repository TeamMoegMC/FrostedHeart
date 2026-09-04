/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.render.weather;

import com.teammoeg.chorda.math.Rect;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.ClimateType;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.InterpolationClimateEvent;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.MutableVisualWeatherSample;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WhiteCurtainDescriptor;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientWeatherStateTest {
    private static ResourceKey<Level> testDimension;
    private final ClientWeatherState state = ClientWeatherState.INSTANCE;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        testDimension = ResourceKey.create(
                Registries.DIMENSION, new ResourceLocation("frostedheart", "weather_state_test"));
    }

    @AfterEach
    void resetSingleton() {
        state.reset();
    }

    @Test
    void pendingSnapshotAppliesAtomicallyOnlyToMatchingDimension() {
        List<WhiteCurtainDescriptor> descriptor = List.of(descriptor(0, 0));
        long generation = state.snapshotGeneration();
        assertFalse(state.receiveSnapshot(testDimension, 1300L, 0L, descriptor, null, 0L));
        assertTrue(state.descriptors().isEmpty());

        state.tick(testDimension, 200L, 8.0, 8.0, ClimateType.NONE, 0, WeatherQualityProfile.FAST);

        assertEquals(descriptor, state.descriptors());
        assertEquals(generation + 1L, state.snapshotGeneration());
        assertTrue(state.hasGrid());
    }

    @Test
    void correctionIsBoundedAndClockCostDoesNotScaleWithFrames() {
        state.replaceSnapshot(testDimension, 100L, 200L, List.of(), 200L);
        state.tick(testDimension, 220L, 0.0, 0.0, ClimateType.NONE, 0, WeatherQualityProfile.FAST);
        assertEquals(101.0, state.frameClimateSeconds(0.0F), 1.0e-6);
        state.correctClock(105L, 220L, 220L);

        state.tick(testDimension, 221L, 0.0, 0.0, ClimateType.NONE, 0, WeatherQualityProfile.FAST);

        assertEquals(101.15, state.frameClimateSeconds(0.0F), 1.0e-6);
        assertEquals(state.frameClimateSeconds(0.5F), state.frameClimateSeconds(0.5F), 0.0);
    }

    @Test
    void repeatedClockCorrectionsReplaceRatherThanAccumulateRemainingError() {
        state.replaceSnapshot(testDimension, 100L, 200L, List.of(), 200L);
        state.tick(testDimension, 220L, 0.0, 0.0, ClimateType.NONE, 0, WeatherQualityProfile.FAST);
        state.correctClock(105L, 220L, 220L);
        state.tick(testDimension, 221L, 0.0, 0.0, ClimateType.NONE, 0, WeatherQualityProfile.FAST);

        state.correctClock(105L, 220L, 221L);

        assertEquals(3.9, state.correctionRemaining(), 1.0e-6);
    }

    @Test
    void frozenDaylightClockDoesNotAdvanceAtTickOrFrameRate() {
        state.replaceSnapshot(testDimension, 100L, 0L, List.of(), 0L);
        state.tick(testDimension, 20L, 0.0, 0.0, ClimateType.NONE, 0, WeatherQualityProfile.FAST);
        state.tick(testDimension, 20L, 0.0, 0.0, ClimateType.NONE, 0, WeatherQualityProfile.FAST);

        assertEquals(101.0, state.frameClimateSeconds(0.5F), 1.0e-6);
    }

    @Test
    void forwardAndBackwardDayTimeJumpsReanchorWithoutDoubleApplying() {
        state.replaceSnapshot(testDimension, 100L, 0L, List.of(), 0L);

        state.correctClock(300L, 4000L, 0L);
        state.tick(testDimension, 4000L, 0.0, 0.0, ClimateType.NONE, 0, WeatherQualityProfile.FAST);
        assertEquals(300.0, state.frameClimateSeconds(0.0F), 1.0e-6);

        state.correctClock(1325L, 500L, 4000L);
        state.tick(testDimension, 500L, 0.0, 0.0, ClimateType.NONE, 0, WeatherQualityProfile.FAST);
        assertEquals(1325.0, state.frameClimateSeconds(0.0F), 1.0e-6);
        state.tick(testDimension, 501L, 0.0, 0.0, ClimateType.NONE, 0, WeatherQualityProfile.FAST);
        assertEquals(1325.05, state.frameClimateSeconds(0.0F), 1.0e-6);
    }

    @Test
    void largeCorrectionReanchorsImmediately() {
        state.replaceSnapshot(testDimension, 100L, 0L, List.of(), 0L);

        state.correctClock(500L, 0L, 0L);

        assertEquals(500.0, state.frameClimateSeconds(0.0F), 1.0e-6);
        assertEquals(0.0, state.correctionRemaining(), 0.0);
    }

    @Test
    void compatibilityTicksAdvanceClockWithoutRebuildingTheSpatialGrid() {
        state.replaceSnapshot(testDimension, 100L, 0L, List.of(), 0L);
        long rebuilds = state.gridRebuilds();
        for (long tick = 1L; tick <= 1000L; tick++) {
            state.tickClock(testDimension, tick);
            state.disableSpatialTick();
        }

        assertEquals(150.0, state.frameClimateSeconds(0.0F), 1.0e-6);
        assertEquals(rebuilds, state.gridRebuilds());
        assertFalse(state.hasGrid());

        state.tick(testDimension, 1001L, 0.0, 0.0,
                ClimateType.SNOW, 0, WeatherQualityProfile.FAST);
        assertEquals(150.05, state.frameClimateSeconds(0.0F), 1.0e-6);
        assertFalse(state.hasGrid());
        assertFalse(state.tickOwnsPrecipitation());
    }

    @Test
    void offscreenDescriptorsDoNotCauseGridFieldEvaluations() {
        List<WhiteCurtainDescriptor> descriptors = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            descriptors.add(descriptor(10000 + i * 100, 10000));
        }
        state.replaceSnapshot(testDimension, 1300L, 0L, descriptors, 0L);
        long checks = state.descriptorChecks();
        long evaluations = state.fieldEvaluations();

        state.tick(testDimension, 1L, 0.0, 0.0, ClimateType.NONE, 0, WeatherQualityProfile.FANCY);

        assertEquals(32L, state.descriptorChecks() - checks);
        assertEquals(0L, state.fieldEvaluations() - evaluations);
        assertEquals(0, state.nearCandidateCount());
        assertEquals(0, state.wallCandidateCount());
    }

    @Test
    void activeWallCandidatesAreNearestFirstRegardlessOfSnapshotOrder() {
        List<WhiteCurtainDescriptor> descriptors = List.of(descriptor(20, 0), descriptor(0, 0));
        state.replaceSnapshot(testDimension, 1300L, 0L, descriptors, 0L);

        state.tick(testDimension, 1L, 8.0, 8.0, ClimateType.NONE, 0, WeatherQualityProfile.FAST);

        assertEquals(2, state.wallCandidateCount());
        assertEquals(0, state.wallKernel(0).minChunkX);
        assertTrue(state.wallCandidateDistanceSquared(0) <= state.wallCandidateDistanceSquared(1));
    }

    @Test
    void gridBuffersAlternateWithoutPerTickAllocationAndInterpolate() {
        state.replaceSnapshot(testDimension, 1300L, 0L, List.of(descriptor(0, 0)), 0L);
        state.tick(testDimension, 1L, 8.0, 8.0, ClimateType.NONE, 0, WeatherQualityProfile.FAST);
        Object firstCurrent = state.currentSnowBacking();
        Object firstPrevious = state.previousSnowBacking();
        state.tick(testDimension, 2L, 9.0, 8.0, ClimateType.NONE, 0, WeatherQualityProfile.FAST);

        assertSame(firstCurrent, state.previousSnowBacking());
        assertSame(firstPrevious, state.currentSnowBacking());
        MutableVisualWeatherSample sample = new MutableVisualWeatherSample();
        state.sampleGrid(8.0, 8.0, 0.5F, sample);
        assertTrue(sample.snowIntensity >= 0.0F && sample.snowIntensity <= 1.0F);
        MutableVisualWeatherSample precipitation = new MutableVisualWeatherSample();
        state.samplePrecipitation(8.0, 8.0, 0.5F, precipitation);
        assertEquals(sample.snowIntensity, precipitation.snowIntensity, 1.0e-6F);
        assertEquals(sample.whiteoutIntensity, precipitation.whiteoutIntensity, 1.0e-6F);
        assertEquals(sample.windIntensity, precipitation.windIntensity, 1.0e-6F);
        assertTrue(state.tickOwnsPrecipitation());
    }

    private static WhiteCurtainDescriptor descriptor(int x, int z) {
        return new WhiteCurtainDescriptor(new Rect(x, z, 20, 20), Direction.SOUTH,
                new InterpolationClimateEvent(
                        1000L, 1100L, -10.0F, 1300L, -50.0F,
                        2000L, 2400L, true, true));
    }
}
