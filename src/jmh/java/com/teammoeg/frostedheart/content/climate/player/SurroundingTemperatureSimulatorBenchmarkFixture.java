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

import java.util.Arrays;

/** Synthetic, registry-backed fixtures for the legacy player sampling benchmark. */
public final class SurroundingTemperatureSimulatorBenchmarkFixture {
    private static final boolean MINECRAFT_BOOTSTRAPPED = bootstrapMinecraft();
    private static final double QUERY_X = 0.5D;
    private static final double QUERY_Y = 0.8D;
    private static final double QUERY_Z = 0.5D;

    private SurroundingTemperatureSimulatorBenchmarkFixture() {
    }

    public static SourceFixture source(String pattern) {
        requireBootstrap();
        @SuppressWarnings("unchecked")
        PalettedContainer<BlockState>[] sections = new PalettedContainer[8];
        for (int index = 0; index < sections.length; index++) {
            sections[index] = new PalettedContainer<>(
                    Block.BLOCK_STATE_REGISTRY,
                    Blocks.AIR.defaultBlockState(),
                    PalettedContainer.Strategy.SECTION_STATES);
        }
        int[] topY = new int[32 * 32];
        switch (pattern) {
            case "all_air" -> Arrays.fill(topY, -32_767);
            case "room" -> buildRoom(sections, topY);
            default -> throw new IllegalArgumentException("unknown fixture pattern: " + pattern);
        }
        return new SourceFixture(sections, topY);
    }

    public static final class SourceFixture {
        private final PalettedContainer<BlockState>[] sections;
        private final int[] topY;

        private SourceFixture(PalettedContainer<BlockState>[] sections, int[] topY) {
            this.sections = sections;
            this.topY = topY;
        }

        public CapturedFixture capture() {
            return new CapturedFixture(
                    SurroundingTemperatureSimulator.prepareForWorker(sections, topY));
        }
    }

    public static final class CapturedFixture {
        private final SurroundingTemperatureSimulator.PreparedInput input;

        private CapturedFixture(SurroundingTemperatureSimulator.PreparedInput input) {
            this.input = input;
        }

        public SurroundingTemperatureSimulator simulator(long seed) {
            return new SurroundingTemperatureSimulator(input, 0, 0, 0, seed);
        }

        public Object retainedGraphRoot() {
            return input;
        }
    }

    public static float simulate(SurroundingTemperatureSimulator simulator) {
        SurroundingTemperatureSimulator.SimulationResult result =
                simulator.getBlockTemperatureAndWind(QUERY_X, QUERY_Y, QUERY_Z);
        return result.blockTemp() + result.windStrength();
    }

    public static void cleanupThreadLocal() {
        SurroundingTemperatureSimulator.cleanup();
    }

    public static void clearBlockInfoCache() {
        CachedBlockTempInfo.clear();
    }

    private static void buildRoom(PalettedContainer<BlockState>[] sections, int[] topY) {
        Arrays.fill(topY, 6);
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState slab = Blocks.OAK_SLAB.defaultBlockState();
        for (int x = -8; x <= 7; x++) {
            for (int z = -8; z <= 7; z++) {
                setBlock(sections, x, -2, z, stone);
                setBlock(sections, x, 5, z, stone);
            }
        }
        for (int y = -1; y <= 4; y++) {
            for (int offset = -8; offset <= 7; offset++) {
                BlockState wall = ((y + offset) & 3) == 0 ? slab : stone;
                setBlock(sections, -8, y, offset, wall);
                setBlock(sections, 7, y, offset, wall);
                setBlock(sections, offset, y, -8, wall);
                setBlock(sections, offset, y, 7, wall);
            }
        }
    }

    private static void setBlock(
            PalettedContainer<BlockState>[] sections,
            int x,
            int y,
            int z,
            BlockState state
    ) {
        int sectionIndex = 0;
        if (x >= 0) {
            sectionIndex |= 4;
        }
        if (z >= 0) {
            sectionIndex |= 2;
        }
        if (y >= 0) {
            sectionIndex |= 1;
        }
        sections[sectionIndex].set(x & 15, y & 15, z & 15, state);
    }

    private static boolean bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        SharedConstants.enableDataFixerOptimizations();
        Bootstrap.bootStrap();
        CommentedConfig serverConfig = CommentedConfig.inMemory();
        FHConfig.SERVER_CONFIG.correct(serverConfig);
        FHConfig.SERVER_CONFIG.setConfig(serverConfig);
        return true;
    }

    private static void requireBootstrap() {
        if (!MINECRAFT_BOOTSTRAPPED) {
            throw new IllegalStateException("Minecraft bootstrap did not complete");
        }
    }
}
