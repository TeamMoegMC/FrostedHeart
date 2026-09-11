/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.gamedata.climate;

import com.teammoeg.chorda.math.Rect;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhiteCurtainFieldModelTest {
    private static final Rect AREA = new Rect(10, 20, 6, 8);

    @Test
    void fourDirectionsMapToLegacyTravelDelay() {
        ClimateEvent event = WhiteCurtainDescriptorCodecTest.event();
        assertEquals(8, WhiteCurtainFieldModel.deltaChunks(descriptor(Direction.NORTH, event), new ChunkPos(12, 20)));
        assertEquals(8, WhiteCurtainFieldModel.deltaChunks(descriptor(Direction.SOUTH, event), new ChunkPos(12, 28)));
        assertEquals(6, WhiteCurtainFieldModel.deltaChunks(descriptor(Direction.WEST, event), new ChunkPos(10, 22)));
        assertEquals(6, WhiteCurtainFieldModel.deltaChunks(descriptor(Direction.EAST, event), new ChunkPos(16, 22)));
    }

    @Test
    void gameplaySamplingMatchesLegacyHourlyCacheForAllDirections() {
        ClimateEvent event = WhiteCurtainDescriptorCodecTest.event();
        long[] seconds = {500L, 951L, 1000L, 1050L, 1300L, 2000L, 2400L, 2401L, 4000L};
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            WhiteCurtainDescriptor descriptor = descriptor(direction, event);
            for (ChunkPos chunk : new ChunkPos[]{new ChunkPos(10, 20), new ChunkPos(13, 24), new ChunkPos(16, 28)}) {
                for (long second : seconds) {
                    assertEquals(legacySample(descriptor, second, chunk),
                            WhiteCurtainFieldModel.sampleGameplay(descriptor, second, chunk),
                            direction + " at " + chunk + " second=" + second);
                }
            }
        }
    }

    @Test
    void visualKernelPhaseAgreesAtChunkCentersAndSampleIsReused() {
        WhiteCurtainDescriptor descriptor = descriptor(Direction.SOUTH, WhiteCurtainDescriptorCodecTest.event());
        WhiteCurtainFieldModel.VisualKernel kernel = WhiteCurtainFieldModel.prepareVisual(descriptor);
        MutableVisualWeatherSample sample = new MutableVisualWeatherSample();
        ChunkPos chunk = new ChunkPos(13, 24);
        long second = 2350L;
        long localSecond = second - WhiteCurtainFieldModel.deltaSeconds(descriptor, chunk);
        int hour = (int) ((localSecond - descriptor.climate().getStartTime()) / WorldClockSource.secondsPerHour);

        assertEquals(WhiteCurtainFieldModel.sampleGameplay(descriptor, second, chunk).climate(), kernel.phaseAt(hour));
        WhiteCurtainFieldModel.sampleVisual(kernel, second, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(),
                WhiteCurtainVisualProfile.FAST, sample);
        assertSame(sample, sample.clear());
        WhiteCurtainFieldModel.sampleVisual(kernel, second, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(),
                WhiteCurtainVisualProfile.FAST, sample);
        assertTrue(sample.insideAffectedCorridor);
        assertTrue(sample.snowIntensity >= 0.0F && sample.snowIntensity <= 1.0F);
        assertTrue(sample.whiteoutIntensity >= 0.0F && sample.whiteoutIntensity <= 1.0F);
    }

    @Test
    void visualCorridorUsesBoundedSmoothEdges() {
        WhiteCurtainFieldModel.VisualKernel kernel = WhiteCurtainFieldModel.prepareVisual(
                descriptor(Direction.EAST, WhiteCurtainDescriptorCodecTest.event()));
        MutableVisualWeatherSample sample = new MutableVisualWeatherSample();

        WhiteCurtainFieldModel.sampleVisual(kernel, 1600.0, kernel.minBlockX - 0.01, kernel.minBlockZ + 64.0,
                WhiteCurtainVisualProfile.FANCY, sample);
        assertFalse(sample.insideAffectedCorridor);
        assertEquals(0.0F, sample.snowIntensity);

        WhiteCurtainFieldModel.sampleVisual(kernel, 1600.0, kernel.minBlockX + 12.0, kernel.minBlockZ + 64.0,
                WhiteCurtainVisualProfile.FANCY, sample);
        assertTrue(sample.insideAffectedCorridor);
        assertTrue(sample.snowIntensity >= 0.0F && sample.snowIntensity <= 1.0F);
        assertTrue(Float.isFinite(sample.visibilityBlocks));
    }

    @Test
    void snowAndWhiteoutPhaseChangesUseTheConfiguredFiveSecondTransition() {
        WhiteCurtainDescriptor descriptor = descriptor(
                Direction.SOUTH, WhiteCurtainDescriptorCodecTest.event());
        WhiteCurtainFieldModel.VisualKernel kernel = WhiteCurtainFieldModel.prepareVisual(descriptor);
        int transitionHour = -1;
        for (int hour = 0; hour < kernel.phaseCount(); hour++) {
            ClimateType previous = kernel.phaseAt(hour - 1);
            ClimateType current = kernel.phaseAt(hour);
            if (previous.isSnowyOrBlizzard() != current.isSnowyOrBlizzard()
                    || whiteout(previous) != whiteout(current)) {
                transitionHour = hour;
                break;
            }
        }
        assertTrue(transitionHour >= 0);
        ChunkPos chunk = new ChunkPos(13, 24);
        double boundary = kernel.startSeconds + transitionHour * WorldClockSource.secondsPerHour
                + WhiteCurtainFieldModel.deltaSeconds(descriptor, chunk);
        MutableVisualWeatherSample sample = new MutableVisualWeatherSample();

        WhiteCurtainFieldModel.sampleVisual(kernel, boundary + 2.5,
                chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(),
                WhiteCurtainVisualProfile.FAST, sample);

        ClimateType previous = kernel.phaseAt(transitionHour - 1);
        ClimateType current = kernel.phaseAt(transitionHour);
        float previousSnow = previous.isSnowyOrBlizzard() ? 1.0F : 0.0F;
        float currentSnow = current.isSnowyOrBlizzard() ? 1.0F : 0.0F;
        assertEquals((previousSnow + currentSnow) * 0.5F, sample.snowIntensity, 1.0e-5F);
        assertEquals((whiteout(previous) + whiteout(current)) * 0.5F,
                sample.whiteoutIntensity, 1.0e-5F);
    }

    @Test
    void inclusiveCurtainBoundsCannotShareAnAuthoritativeChunk() {
        ClimateEvent event = WhiteCurtainDescriptorCodecTest.event();
        WhiteCurtainDescriptor first = new WhiteCurtainDescriptor(
                new Rect(0, 0, 2, 2), Direction.EAST, event);

        assertTrue(WhiteCurtainFieldModel.isIntersected(first, new Rect(2, 0, 2, 2)));
        assertFalse(WhiteCurtainFieldModel.isIntersected(first, new Rect(3, 0, 2, 2)));
    }

    @Test
    void forecastCacheIncludesInclusiveTravelEndpoint() {
        WhiteCurtainInfo curtain = new WhiteCurtainInfo(
                descriptor(Direction.SOUTH, WhiteCurtainDescriptorCodecTest.event()));

        assertEquals(curtain.getMaxDelta() + 1, curtain.forecastCache.size());
    }

    private static WhiteCurtainDescriptor descriptor(Direction direction, ClimateEvent event) {
        return new WhiteCurtainDescriptor(AREA, direction, event);
    }

    private static ClimateResult legacySample(WhiteCurtainDescriptor descriptor, long second, ChunkPos chunk) {
        long local = second - WhiteCurtainFieldModel.deltaChunks(descriptor, chunk)
                * WhiteCurtainInfo.getSecondsPerChunk();
        ClimateEvent event = descriptor.climate();
        if (local > event.getCalmEndTime()) {
            return ClimateResult.EMPTY;
        }
        long hour = (local - event.getStartTime()) / WorldClockSource.secondsPerHour;
        long emittedHours = (event.getCalmEndTime() - event.getStartTime()) / WorldClockSource.secondsPerHour;
        if (hour < 0 || hour >= emittedHours) {
            return ClimateResult.EMPTY;
        }
        return event.getHourClimate(event.getStartTime() + hour * WorldClockSource.secondsPerHour);
    }

    private static float whiteout(ClimateType phase) {
        return phase.isBlizzard() ? 1.0F : phase == ClimateType.SNOW_BLIZZARD ? 0.35F : 0.0F;
    }
}
