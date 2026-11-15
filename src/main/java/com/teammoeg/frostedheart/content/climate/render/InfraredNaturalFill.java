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

package com.teammoeg.frostedheart.content.climate.render;

import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.stream.IntStream;

import org.lwjgl.BufferUtils;

import com.teammoeg.frostedheart.content.climate.ClimateCoverChunk;
import com.teammoeg.frostedheart.content.climate.ClimateCoverColumns;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.data.BiomeTempData;
import com.teammoeg.frostedheart.content.climate.data.WorldTempData;
import com.teammoeg.frostedheart.content.climate.network.InfraredBrickCodec;
import com.teammoeg.frostedheart.content.climate.network.InfraredNaturalClimate;
import com.teammoeg.frostedheart.content.climate.network.InfraredWindow;

import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * 红外窗口里服务端没有发送温度的方块的自然温度，客户端按自己的世界计算：维度、Brick 中心的群系、海拔、覆盖层，加上服务端发来的
 * 覆盖层气候。与热场的背景 {@code NaturalProfile} 是同一组项，所以静止的热场和它周围读数一致。每 tick 只算有限个 Page，近的先算。
 * <p>
 * Natural temperature of the infrared window's blocks the server sends no value for, computed on the client from its
 * own world: dimension, biome at each Brick centre, altitude and cover, with the covered climate the server sends.
 * These are the terms of {@code NaturalProfile}, the thermal field's background, so a field at rest and its
 * surroundings read alike. Air stays without a value. A few Pages are computed per client tick, nearest first.
 */
final class InfraredNaturalFill {
    private static final int WIDTH = InfraredWindow.WIDTH, SIDE = InfraredWindow.SIDE, PAGES = InfraredWindow.PAGES;
    private static final int WORDS = (PAGES + 63) / 64;
    /** Pages computed per client tick; a solid Page costs about a tenth of a millisecond. */
    private static final int PAGES_PER_TICK = 32;
    private static final int[] NEAREST_FIRST = IntStream.range(0, PAGES).boxed()
            .sorted(java.util.Comparator.comparingInt(InfraredNaturalFill::distanceFromCentre)).mapToInt(Integer::intValue)
            .toArray();

    private ShortBuffer values;
    private long[] dirty = new long[WORDS];
    private final long[] computed = new long[WORDS];
    private InfraredNaturalClimate climate;
    private long center;
    private boolean centered;
    private final float[] brickBase = new float[64];
    private final IdentityHashMap<Biome, Float> biomeTemperatures = new IdentityHashMap<>();
    private final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

    private static int distanceFromCentre(int page) {
        int x = page % WIDTH - WIDTH / 2, z = page / WIDTH % WIDTH - WIDTH / 2, y = page / (WIDTH * WIDTH) - WIDTH / 2;
        return x * x + y * y + z * z;
    }

    /** Revision of the covered climate held, 0 for none. */
    long revision() {
        return climate == null ? 0 : climate.revision();
    }

    boolean hasValues() {
        return values != null;
    }

    /** Natural value of one texel of the window, or {@link InfraredBrickCodec#INVALID_TEMPERATURE}. */
    short at(int texel) {
        return values == null ? InfraredBrickCodec.INVALID_TEMPERATURE : values.get(texel);
    }

    /** A new covered climate reaches every Page. */
    void accept(InfraredNaturalClimate next) {
        if (next == null || climate != null && climate.revision() == next.revision()) return;
        climate = next;
        Arrays.fill(dirty, -1L);
    }

    /** The window moved: the overlap keeps its values, and the Pages that entered are computed. */
    void recenter(long next) {
        if (centered && center == next) return;
        if (values != null && centered && InfraredWindow.overlaps(center, next)) {
            InfraredWindow.shift(values, center, next);
            long[] carried = new long[WORDS];
            InfraredWindow.remap(dirty, carried, center, next);
            for (int page = 0; page < PAGES; page++)
                if (InfraredWindow.previousPage(page, center, next) < 0) carried[page >>> 6] |= 1L << (page & 63);
            dirty = carried;
        } else {
            if (values != null) clear(values);
            Arrays.fill(dirty, -1L);
        }
        Arrays.fill(computed, 0L);
        center = next;
        centered = true;
    }

    /** A block changed; when its opacity changed the cover of every block below it, so those Pages are recomputed. */
    void blockChanged(int x, int y, int z, boolean coverChanged) {
        if (!centered) return;
        int pageX = (x >> 4) - SectionPos.x(center) + WIDTH / 2, pageZ = (z >> 4) - SectionPos.z(center) + WIDTH / 2;
        int pageY = (y >> 4) - SectionPos.y(center) + WIDTH / 2;
        if (pageX < 0 || pageX >= WIDTH || pageZ < 0 || pageZ >= WIDTH || pageY < 0) return;
        int lowest = coverChanged ? 0 : pageY;
        for (int layer = lowest; layer <= Math.min(pageY, WIDTH - 1); layer++) mark(pageX + WIDTH * (pageZ + WIDTH * layer));
    }

    /** A chunk arrived; its Pages could not be computed before. */
    void chunkLoaded(int chunkX, int chunkZ) {
        if (!centered) return;
        int pageX = chunkX - SectionPos.x(center) + WIDTH / 2, pageZ = chunkZ - SectionPos.z(center) + WIDTH / 2;
        if (pageX < 0 || pageX >= WIDTH || pageZ < 0 || pageZ >= WIDTH) return;
        for (int page = 0; page < WIDTH; page++) mark(pageX + WIDTH * (pageZ + WIDTH * page));
    }

    /**
     * Computes up to {@link #PAGES_PER_TICK} waiting Pages, nearest first. Pages of chunks the client has not loaded
     * keep waiting. Returns whether any Page changed; {@link #takeComputed} names them.
     */
    boolean advance(Level level) {
        if (climate == null || !centered) return false;
        if (values == null) {
            values = BufferUtils.createShortBuffer(SIDE * SIDE * SIDE);
            clear(values);
        }
        int budget = PAGES_PER_TICK;
        boolean changed = false;
        for (int page : NEAREST_FIRST) {
            if (budget == 0) break;
            if ((dirty[page >>> 6] & 1L << (page & 63)) == 0 || !compute(level, page)) continue;
            dirty[page >>> 6] &= ~(1L << (page & 63));
            computed[page >>> 6] |= 1L << (page & 63);
            budget--;
            changed = true;
        }
        return changed;
    }

    /** Pages computed since the last call, for upload. */
    long[] takeComputed() {
        long[] pages = computed.clone();
        Arrays.fill(computed, 0L);
        return pages;
    }

    void reset() {
        values = null;
        climate = null;
        centered = false;
        Arrays.fill(dirty, 0L);
        Arrays.fill(computed, 0L);
        biomeTemperatures.clear();
    }

    private void mark(int page) {
        dirty[page >>> 6] |= 1L << (page & 63);
    }

    private boolean compute(Level level, int page) {
        int pageX = page % WIDTH, pageZ = page / WIDTH % WIDTH, pageY = page / (WIDTH * WIDTH);
        int sectionX = SectionPos.x(center) - WIDTH / 2 + pageX, sectionZ = SectionPos.z(center) - WIDTH / 2 + pageZ;
        int sectionY = SectionPos.y(center) - WIDTH / 2 + pageY;
        LevelChunk chunk = level.getChunkSource().getChunk(sectionX, sectionZ, false);
        if (chunk == null) return false;
        clearPage(pageX, pageY, pageZ);
        if (sectionY < chunk.getMinSection() || sectionY >= chunk.getMaxSection()) return true;
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
        if (section.hasOnlyAir()) return true;
        int minX = SectionPos.sectionToBlockCoord(sectionX), minY = SectionPos.sectionToBlockCoord(sectionY);
        int minZ = SectionPos.sectionToBlockCoord(sectionZ);
        float dimension = WorldTempData.getWorldTemp(level);
        for (int brick = 0; brick < 64; brick++) {
            position.set(minX + ((brick & 3) << 2) + 2, minY + ((brick >>> 4) << 2) + 2, minZ + ((brick >>> 2 & 3) << 2) + 2);
            brickBase[brick] = dimension + biomeTemperature(level, level.getBiome(position).value());
        }
        ClimateCoverColumns columns = ((ClimateCoverChunk) chunk).frostedheart$getClimateCover();
        int limit = climate.coverLevels() - 1, windowChunk = pageX + WIDTH * pageZ;
        for (int y = 0; y < 16; y++) {
            float altitude = WorldTemperature.altitude(minY + y, climate.noUnderground());
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++) {
                    BlockState state = section.getBlockState(x, y, z);
                    if (state.isAir()) continue;
                    int cover = columns.coverAbove(chunk, minX + x, minY + y, minZ + z, limit);
                    int brick = x >>> 2 | (z >>> 2) << 2 | (y >>> 2) << 4;
                    double natural = Math.max(climate.absoluteZeroC(),
                            brickBase[brick] + altitude + climate.climate(windowChunk, cover));
                    values.put(texel(pageX * 16 + x, pageY * 16 + y, pageZ * 16 + z), InfraredBrickCodec.quantize(natural));
                }
        }
        return true;
    }

    /** Biome data is read directly: {@code WorldTemperature}'s caches belong to the server thread. */
    private float biomeTemperature(Level level, Biome biome) {
        Float cached = biomeTemperatures.get(biome);
        if (cached == null) biomeTemperatures.put(biome, cached = BiomeTempData.getBiomeTemp(level, biome));
        return cached;
    }

    private void clearPage(int pageX, int pageY, int pageZ) {
        for (int z = 0; z < 16; z++)
            for (int y = 0; y < 16; y++) {
                int row = texel(pageX * 16, pageY * 16 + y, pageZ * 16 + z);
                for (int x = 0; x < 16; x++) values.put(row + x, InfraredBrickCodec.INVALID_TEMPERATURE);
            }
    }

    static int texel(int x, int y, int z) {
        return (z * SIDE + y) * SIDE + x;
    }

    private static void clear(ShortBuffer buffer) {
        for (int index = 0; index < buffer.capacity(); index++) buffer.put(index, InfraredBrickCodec.INVALID_TEMPERATURE);
    }
}
