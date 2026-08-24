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
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhiteCurtainCacheTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void addAndClearInvalidateCachedChunkImmediately() {
        WorldClimate climate = new WorldClimate(0);
        ChunkPos chunk = new ChunkPos(0, 0);
        climate.clockSource.secs = 1300L;
        assertEquals(ClimateType.NONE, climate.getClimate(chunk));
        long initialGeneration = climate.getWhiteCurtainGeneration();

        WhiteCurtainInfo curtain = new WhiteCurtainInfo(new WhiteCurtainDescriptor(
                new Rect(0, 0, 2, 2), Direction.SOUTH, WhiteCurtainDescriptorCodecTest.event()));
        assertTrue(climate.tryAddWhiteCurtain(curtain));
        assertEquals(initialGeneration + 1L, climate.getWhiteCurtainGeneration());
        assertEquals(curtain.getClimate(1300L, chunk).climate(), climate.getClimate(chunk));

        climate.clearWhiteCurtain();
        assertEquals(initialGeneration + 2L, climate.getWhiteCurtainGeneration());
        assertEquals(ClimateType.NONE, climate.getClimate(chunk));
    }

    @Test
    void naturalPruneInvalidatesOnlyWhenListChanges() {
        WorldClimate climate = new WorldClimate(0);
        WhiteCurtainDescriptor descriptor = new WhiteCurtainDescriptor(
                new Rect(0, 0, 2, 2), Direction.EAST, WhiteCurtainDescriptorCodecTest.event());
        assertTrue(climate.tryAddWhiteCurtain(new WhiteCurtainInfo(descriptor)));
        long generation = climate.getWhiteCurtainGeneration();

        assertFalse(climate.pruneInvalidWhiteCurtains(descriptor.climate().getCalmEndTime()));
        assertEquals(generation, climate.getWhiteCurtainGeneration());
        assertTrue(climate.pruneInvalidWhiteCurtains(
                descriptor.climate().getCalmEndTime() + WhiteCurtainFieldModel.maxDeltaSeconds(descriptor) + 1L));
        assertEquals(generation + 1L, climate.getWhiteCurtainGeneration());
    }

    @Test
    void transitionTimeAdvancesWithoutHourlyGlobalCacheClear() {
        WhiteCurtainDescriptor descriptor = new WhiteCurtainDescriptor(
                new Rect(0, 0, 1, 1), Direction.SOUTH, WhiteCurtainDescriptorCodecTest.event());
        ChunkPos chunk = new ChunkPos(0, 0);
        long before = 1049L;
        assertEquals(before + 1L, WhiteCurtainFieldModel.nextGameplayTransition(descriptor, before, chunk));
        assertFalse(WhiteCurtainFieldModel.sampleGameplay(descriptor, before, chunk).equals(
                WhiteCurtainFieldModel.sampleGameplay(descriptor, before + 1L, chunk)));
    }
}
