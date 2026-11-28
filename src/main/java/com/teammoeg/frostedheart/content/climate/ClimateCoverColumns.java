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

import java.util.Arrays;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 一个区块里每一列的覆盖层：自上而下最多记录若干个不透光满方块的高度，首次查询时扫描，列内不透光性变化时丢弃。
 * <p>
 * Cover of each column of one chunk: the heights of up to the cover limit of opaque full blocks, top-down. Scanned on
 * first query and dropped when a block in the column changes opacity at or above the deepest recorded block.
 */
public final class ClimateCoverColumns {
    private final short[][] columns = new short[256][];
    private int limit;

    /** Opaque full blocks above {@code y} in the column at {@code (x, z)}, at most {@code limit}. */
    public int coverAbove(LevelChunk chunk, int x, int y, int z, int limit) {
        if (limit != this.limit) {
            Arrays.fill(columns, null);
            this.limit = limit;
        }
        int column = (x & 15) | (z & 15) << 4;
        short[] heights = columns[column];
        if (heights == null) columns[column] = heights = scan(chunk, x, z, limit);
        int cover = 0;
        while (cover < heights.length && heights[cover] > y) cover++;
        return cover;
    }

    /**
     * A block at {@code y} changed opacity. Returns whether any cover in the column may have changed: below a full
     * record every position keeps its capped cover, and a column never scanned has nothing to drop.
     */
    public boolean changed(int x, int y, int z) {
        int column = (x & 15) | (z & 15) << 4;
        short[] heights = columns[column];
        if (heights == null || heights.length == limit && y < heights[heights.length - 1]) return false;
        columns[column] = null;
        return true;
    }

    private static short[] scan(LevelChunk chunk, int x, int z, int limit) {
        short[] heights = new short[limit];
        int count = 0;
        int minimum = chunk.getMinBuildHeight();
        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        while (y >= minimum && count < limit) {
            int index = chunk.getSectionIndex(y);
            LevelChunkSection section = sections[index];
            if (section.hasOnlyAir()) {
                y = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index)) - 1;
                continue;
            }
            BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
            if (state.isSolidRender(chunk, cursor.set(x, y, z))) heights[count++] = (short) y;
            y--;
        }
        return count == limit ? heights : Arrays.copyOf(heights, count);
    }
}
