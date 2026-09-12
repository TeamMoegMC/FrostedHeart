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
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WhiteCurtainDescriptor;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WhiteCurtainVisualProfile;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientWeatherFrameTest {
    private static ResourceKey<Level> testDimension;
    private final ClientWeatherState state = ClientWeatherState.INSTANCE;
    private final ClientWeatherFrame frame = ClientWeatherFrame.INSTANCE;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        testDimension = ResourceKey.create(
                Registries.DIMENSION, new ResourceLocation("frostedheart", "weather_frame_test"));
    }

    @AfterEach
    void resetSingletons() {
        state.reset();
        frame.invalidate();
    }

    @Test
    void compatibilityAndMissingStateSelectFallback() {
        frame.begin(state, WeatherRenderingMode.COMPATIBILITY, 0.0, 64.0, 0.0, 0.0F);
        assertEquals(ClientWeatherFrame.Ownership.FALLBACK, frame.ownership());
        assertFalse(frame.valid());
    }

    @Test
    void ordinarySnowAndBlizzardKeepVanillaOwnership() {
        state.replaceSnapshot(testDimension, 1300L, 0L, List.of(), 0L);
        state.tick(testDimension, 1L, 0.0, 0.0, ClimateType.SNOW, 50, WeatherQualityProfile.FAST);

        frame.begin(state, WeatherRenderingMode.SPATIAL_V1_FAST, 0.0, 64.0, 0.0, 0.5F);

        assertFalse(frame.valid());
        assertEquals(ClientWeatherFrame.Ownership.FALLBACK, frame.ownership());
        assertFalse(frame.ownsPrecipitation());

        state.tick(testDimension, 2L, 0.0, 0.0, ClimateType.BLIZZARD, 100, WeatherQualityProfile.FANCY);
        frame.begin(state, WeatherRenderingMode.SPATIAL_V1_FANCY, 0.0, 64.0, 0.0, 0.5F);
        assertEquals(ClientWeatherFrame.Ownership.FALLBACK, frame.ownership());
        assertFalse(frame.ownsPrecipitation());
    }

    @Test
    void distantCandidateKeepsVanillaPrecipitationInWallOnlyMode() {
        state.replaceSnapshot(testDimension, 1300L, 0L, List.of(descriptorAtChunk(20)), 0L);
        state.tick(testDimension, 1L, 0.0, 0.0, ClimateType.BLIZZARD, 100, WeatherQualityProfile.FANCY);

        frame.begin(state, WeatherRenderingMode.SPATIAL_V1_FANCY, 0.0, 64.0, 0.0, 0.0F);

        assertEquals(ClientWeatherFrame.Ownership.WALL_ONLY, frame.ownership());
        assertFalse(frame.ownsPrecipitation());
    }

    @Test
    void fixedProfilesRespectInitialHardCaps() {
        assertTrue(WeatherQualityProfile.FAST.wallSlices() <= 12);
        assertTrue(WeatherQualityProfile.FANCY.wallSlices() <= 12);
        assertTrue(WeatherQualityProfile.FAST.precipitationColumns() <= 256);
        assertTrue(WeatherQualityProfile.FANCY.precipitationColumns() <= 1024);
        assertTrue(WeatherQualityProfile.FAST.terrainQueriesPerTick() <= 12);
        assertTrue(WeatherQualityProfile.FANCY.terrainQueriesPerTick() <= 32);
        assertEquals(36, WeatherQualityProfile.FAST.wallSlices() * WeatherQualityProfile.FAST.wallSegments());
        assertEquals(100, WeatherQualityProfile.FANCY.wallSlices() * WeatherQualityProfile.FANCY.wallSegments());
        assertEquals(2, SpatialWeatherRenderer.wallSlicesForWall(0, 2, 4));
        assertEquals(2, SpatialWeatherRenderer.wallSlicesForWall(1, 2, 4));
        assertEquals(0, SpatialWeatherRenderer.wallSlicesForWall(2, 2, 4));
        assertEquals(16.0, SpatialWeatherRenderer.wallSegmentDistanceSquared(
                4.0, 4.0, -1000.0, 0.0, 1000.0, 0.0), 1.0e-6);
        assertEquals(100.0, SpatialWeatherRenderer.wallSegmentDistanceSquared(
                10.0, 4.0, 0.0, -1000.0, 0.0, 1000.0), 1.0e-6);
        assertEquals(0.0F, SpatialWeatherRenderer.wallEdgeFade(0.0, 0.0, 128.0, 48.0), 1.0e-6F);
        assertEquals(0.5F, SpatialWeatherRenderer.wallEdgeFade(24.0, 0.0, 128.0, 48.0), 1.0e-6F);
        assertEquals(1.0F, SpatialWeatherRenderer.wallEdgeFade(64.0, 0.0, 128.0, 48.0), 1.0e-6F);
        assertEquals(1.0F, SpatialWeatherRenderer.weatherBrightness(1.0F), 1.0e-6F);
        assertEquals(0.64F, SpatialWeatherRenderer.weatherBrightness(0.2F), 1.0e-6F);
        assertEquals(16.0F, WhiteCurtainVisualProfile.FAST.minimumVisibilityBlocks());
        assertEquals(16.0F, WhiteCurtainVisualProfile.FANCY.minimumVisibilityBlocks());
    }

    @Test
    void indoorExposureFadesEffectsWithoutReturningWorkToCompatibility() {
        state.replaceSnapshot(testDimension, 1300L, 0L, List.of(descriptorAtChunk(0)), 0L);
        for (int tick = 1; tick <= 7; tick++) {
            state.tick(testDimension, tick, 0.0, 0.0, ClimateType.NONE, 0,
                    WeatherQualityProfile.FAST, false);
        }

        frame.begin(state, WeatherRenderingMode.SPATIAL_V1_FAST, 0.0, 64.0, 0.0, 0.0F);

        assertEquals(0.0F, state.cameraExposure(), 1.0e-6F);
        assertTrue(state.tickOwnsPrecipitation());
        assertEquals(ClientWeatherFrame.Ownership.CUSTOM, frame.ownership());
        assertEquals(0.0F, frame.cameraSample().snowIntensity, 1.0e-6F);
    }

    @Test
    void nearbySnowFootprintIsRenderedBeforeTheCameraCrossesTheFront() {
        state.replaceSnapshot(testDimension, 1300L, 0L, List.of(descriptorAtChunk(0)), 0L);
        state.tick(testDimension, 1L, -1.0, 8.0, ClimateType.NONE, 0, WeatherQualityProfile.FAST);

        frame.begin(state, WeatherRenderingMode.SPATIAL_V1_FAST, -1.0, 64.0, 8.0, 0.5F);

        assertEquals(0.0F, frame.cameraSample().snowIntensity, 1.0e-6F);
        assertTrue(state.hasPrecipitationFootprint());
        assertEquals(ClientWeatherFrame.Ownership.CUSTOM, frame.ownership());
    }

    private static WhiteCurtainDescriptor descriptorAtChunk(int x) {
        return new WhiteCurtainDescriptor(new Rect(x, 0, 8, 8), Direction.EAST,
                new InterpolationClimateEvent(
                        1000L, 1100L, -10.0F, 1300L, -50.0F,
                        2000L, 2400L, true, true));
    }
}
