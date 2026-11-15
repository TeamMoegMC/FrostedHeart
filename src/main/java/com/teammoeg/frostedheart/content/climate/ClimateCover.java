/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.climate;

import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * 气候隔离的覆盖层：某位置正上方这一列里不透光满方块的数量，封顶。只有真实的覆盖层隔温；玻璃、树叶、冰、液体和空腔都不算。
 * <p>
 * Climate cover: the number of opaque full blocks directly above a position in its column, capped. Only real
 * overburden insulates; glass, leaves, ice, liquids and air gaps do not count.
 */
public final class ClimateCover {
    /** A block changed on the client, in a chunk whose cover the client has read. */
    @FunctionalInterface
    public interface ClientBlockChanges {
        void changed(int x, int y, int z, boolean coverChanged);
    }

    /** Set by client code that keeps values derived from cover; never set on a dedicated server. */
    public static volatile ClientBlockChanges clientBlockChanges;

    private ClimateCover() {
    }

    /** Cover beyond which climate isolation no longer grows. */
    public static int limit() {
        return FHConfig.SERVER.CLIMATE.coverMaximumBlocks.get();
    }

    /**
     * Cover above {@code pos}. Unloaded columns and readers other than the server level on its own thread count as
     * uncovered, so the climate is never kept out by a column nobody has loaded.
     */
    public static int coverAbove(LevelReader level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) return 0;
        LevelChunk chunk = server.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        if (chunk == null) return 0;
        return ((ClimateCoverChunk) chunk).frostedheart$getClimateCover()
                .coverAbove(chunk, pos.getX(), pos.getY(), pos.getZ(), limit());
    }
}
