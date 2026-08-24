/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.benchmark;

import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.ChunkHeatData;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.CubicHeatArea;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.IHeatArea;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;

import java.util.ArrayList;
import java.util.List;

final class LegacyChunkHeatFixtures {
    private static final boolean MINECRAFT_BOOTSTRAPPED = bootstrapMinecraft();
    private static final BlockPos HIT_POSITION = new BlockPos(0, 64, 0);
    private static final BlockPos MISS_POSITION = new BlockPos(1_000, 64, 1_000);

    private LegacyChunkHeatFixtures() {
    }

    static ChunkHeatData data(int adjusterCount) {
        if (!MINECRAFT_BOOTSTRAPPED) {
            throw new IllegalStateException("Minecraft bootstrap did not complete");
        }
        if (adjusterCount < 0) {
            throw new IllegalArgumentException("adjusterCount must be non-negative");
        }
        List<IHeatArea> adjusters = new ArrayList<>(adjusterCount);
        for (int index = 0; index < adjusterCount; index++) {
            int x = index % 10;
            int z = index / 10;
            adjusters.add(new CubicHeatArea(new BlockPos(x, 64, z), 16, index + 1));
        }
        return new ChunkHeatData(adjusters);
    }

    static BlockPos hitPosition() {
        return HIT_POSITION;
    }

    static BlockPos missPosition() {
        return MISS_POSITION;
    }

    private static boolean bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        SharedConstants.enableDataFixerOptimizations();
        Bootstrap.bootStrap();
        return true;
    }
}
