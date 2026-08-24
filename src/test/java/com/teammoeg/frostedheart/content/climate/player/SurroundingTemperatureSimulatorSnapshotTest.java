/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.player;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SurroundingTemperatureSimulatorSnapshotTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        SharedConstants.enableDataFixerOptimizations();
        Bootstrap.bootStrap();
        CommentedConfig serverConfig = CommentedConfig.inMemory();
        FHConfig.SERVER_CONFIG.correct(serverConfig);
        FHConfig.SERVER_CONFIG.setConfig(serverConfig);
    }

    @Test
    void preparedInputOwnsSectionAndHeightSnapshots() {
        PalettedContainer<BlockState>[] source = allAirSections();
        source[0].set(15, 15, 15, Blocks.STONE.defaultBlockState());
        int[] topY = new int[32 * 32];
        Arrays.fill(topY, -32_767);

        SurroundingTemperatureSimulator.PreparedInput input =
                SurroundingTemperatureSimulator.prepareForWorker(source, topY);
        source[0].set(15, 15, 15, Blocks.DIRT.defaultBlockState());
        Arrays.fill(topY, 32_767);

        SurroundingTemperatureSimulator simulator =
                new SurroundingTemperatureSimulator(input, 0, 0, 0, 1L);
        SurroundingTemperatureSimulator.SimulationResult result =
                simulator.getBlockTemperatureAndWind(0.5D, 0.8D, 0.5D);

        assertSame(Blocks.STONE.defaultBlockState(), simulator.getBlock(-1, -1, -1));
        assertEquals(40.0F, result.windStrength());
    }

    @Test
    void identicalPreparedInputAndSeedAreDeterministic() {
        int[] topY = new int[32 * 32];
        Arrays.fill(topY, -32_767);
        SurroundingTemperatureSimulator.PreparedInput input =
                SurroundingTemperatureSimulator.prepareForWorker(allAirSections(), topY);

        SurroundingTemperatureSimulator.SimulationResult first =
                new SurroundingTemperatureSimulator(input, 0, 0, 0, 42L)
                        .getBlockTemperatureAndWind(0.5D, 0.8D, 0.5D);
        SurroundingTemperatureSimulator.SimulationResult second =
                new SurroundingTemperatureSimulator(input, 0, 0, 0, 42L)
                        .getBlockTemperatureAndWind(0.5D, 0.8D, 0.5D);

        assertEquals(first, second);
    }

    @Test
    void preparedInputRejectsIncompleteArrays() {
        assertThrows(IllegalArgumentException.class,
                () -> SurroundingTemperatureSimulator.prepareForWorker(
                        Arrays.copyOf(allAirSections(), 7), new int[32 * 32]));
        assertThrows(IllegalArgumentException.class,
                () -> SurroundingTemperatureSimulator.prepareForWorker(
                        allAirSections(), new int[32 * 32 - 1]));
    }

    @SuppressWarnings("unchecked")
    private static PalettedContainer<BlockState>[] allAirSections() {
        PalettedContainer<BlockState>[] sections = new PalettedContainer[8];
        for (int index = 0; index < sections.length; index++) {
            sections[index] = new PalettedContainer<>(
                    Block.BLOCK_STATE_REGISTRY,
                    Blocks.AIR.defaultBlockState(),
                    PalettedContainer.Strategy.SECTION_STATES);
        }
        return sections;
    }
}
