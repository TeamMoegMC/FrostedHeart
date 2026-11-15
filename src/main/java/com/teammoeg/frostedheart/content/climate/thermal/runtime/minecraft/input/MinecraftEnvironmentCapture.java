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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input;

import com.teammoeg.frostedheart.content.climate.ClimateCover;
import com.teammoeg.frostedheart.content.climate.ClimateCoverChunk;
import com.teammoeg.frostedheart.content.climate.ClimateCoverColumns;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.solver.NaturalProfile;
import com.teammoeg.frostedheart.content.climate.thermal.solver.NaturalProfiles;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Arrays;

/**
 * Bounded main-thread natural-temperature and sky-column capture. Each tracked Page also publishes its per-block
 * natural profile: refreshed with the natural cadence when the covered climate moves, and per column when cover
 * changes.
 */
public final class MinecraftEnvironmentCapture implements AutoCloseable {
    private static final long REFRESH_INTERVAL_TICKS = 200L;
    private static final int MAX_NATURAL_REFRESHES_PER_TICK = 16;
    private static final int MAX_SKY_COLUMNS_PER_TICK = 64;
    private static final int MAX_COVER_COLUMNS_PER_TICK = 64;
    private static final double NATURAL_DELTA_C = 0.25D;
    private static final double CLIMATE_DELTA_C = 0.05D;
    private static final double WIND_GAIN = 0.8D;
    private static final double WIND_SCALE_DELTA = 0.05D;

    private final ServerLevel level;
    private final NaturalProfiles profiles;
    private DimensionInputAccumulator accumulator;
    private final Long2ObjectOpenHashMap<Entry> entries =
            new Long2ObjectOpenHashMap<>();
    private final PriorityQueue<Entry> naturalQueue =
            new PriorityQueue<>(Comparator
                    .comparingLong((Entry entry) -> entry.nextRefreshTick)
                    .thenComparingLong(entry -> entry.handle.sectionKey()));
    private final LongLinkedOpenHashSet dirtyColumns =
            new LongLinkedOpenHashSet();
    private final LongLinkedOpenHashSet dirtyCoverColumns =
            new LongLinkedOpenHashSet();
    private final BlockPos.MutableBlockPos temperaturePosition =
            new BlockPos.MutableBlockPos();
    private long nextWindRefreshTick;
    private double capturedWindScale = Double.NaN;

    public MinecraftEnvironmentCapture(
            ServerLevel level,
            DimensionInputAccumulator accumulator,
            NaturalProfiles profiles
    ) {
        this.level = level;
        this.accumulator = accumulator;
        this.profiles = profiles;
    }

    Captured capture(long sectionKey, LevelChunk chunk, long residentBrickMask) {
        int sectionX = SectionPos.x(sectionKey);
        int sectionY = SectionPos.y(sectionKey);
        int sectionZ = SectionPos.z(sectionKey);
        if (chunk.getPos().x != sectionX || chunk.getPos().z != sectionZ) {
            throw new IllegalArgumentException(
                    "environment chunk does not own its Page");
        }
        double natural = naturalTemperature(sectionKey);
        byte[] sky = new byte[256];
        Arrays.fill(sky, (byte) 16);
        int minX = SectionPos.sectionToBlockCoord(sectionX);
        int minY = SectionPos.sectionToBlockCoord(sectionY);
        int minZ = SectionPos.sectionToBlockCoord(sectionZ);
        // Every captured column can have an exposed sparse upper boundary, even below the Section roof.
        long topBricks = (residentBrickMask | residentBrickMask >>> 16 | residentBrickMask >>> 32 | residentBrickMask >>> 48) & 0xffffL;
        while (topBricks != 0L) {
            int brick = Long.numberOfTrailingZeros(topBricks);
            int brickMinX = (brick & 3) << 2;
            int brickMinZ = (brick >>> 2 & 3) << 2;
            for (int z = brickMinZ; z < brickMinZ + 4; z++) {
                for (int x = brickMinX; x < brickMinX + 4; x++) {
                    int exposedY = chunk.getHeight(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            minX + x,
                            minZ + z);
                    sky[x | z << 4] = (byte) Math.max(
                            0, Math.min(16, exposedY - minY));
                }
            }
            topBricks &= topBricks - 1L;
        }
        return new Captured(natural, sky, naturalProfile(sectionKey, chunk));
    }

    /** {@code WorldTemperature.naturalAir} of every block, split into what changes on different clocks. */
    private NaturalProfile naturalProfile(long sectionKey, LevelChunk chunk) {
        int minX = SectionPos.sectionToBlockCoord(SectionPos.x(sectionKey));
        int minY = SectionPos.sectionToBlockCoord(SectionPos.y(sectionKey));
        int minZ = SectionPos.sectionToBlockCoord(SectionPos.z(sectionKey));
        float dimension = WorldTemperature.dimension(level);
        float[] brickBase = new float[64];
        for (int brick = 0; brick < 64; brick++) {
            temperaturePosition.set(minX + ((brick & 3) << 2) + 2, minY + ((brick >>> 4) << 2) + 2,
                    minZ + ((brick >>> 2 & 3) << 2) + 2);
            brickBase[brick] = dimension + WorldTemperature.biome(level, temperaturePosition);
        }
        byte[] cover = new byte[4096];
        ClimateCoverColumns columns = ((ClimateCoverChunk) chunk).frostedheart$getClimateCover();
        int limit = ClimateCover.limit();
        for (int y = 0; y < 16; y++)
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++)
                    cover[x | z << 4 | y << 8] = (byte) columns.coverAbove(chunk, minX + x, minY + y, minZ + z, limit);
        return new NaturalProfile(brickBase, cover, coveredClimate(sectionKey, limit), minY,
                WorldTemperature.hasNoUnderground(level),
                FHConfig.SERVER.CLIMATE.absoluteZeroCelsius.get().floatValue());
    }

    /** Climate reaching each cover in this Section's chunk; index 0 is the open air. */
    private float[] coveredClimate(long sectionKey, int limit) {
        temperaturePosition.set(
                SectionPos.sectionToBlockCoord(SectionPos.x(sectionKey)) + 8,
                SectionPos.sectionToBlockCoord(SectionPos.y(sectionKey)) + 8,
                SectionPos.sectionToBlockCoord(SectionPos.z(sectionKey)) + 8);
        float[] climate = new float[limit + 1];
        for (int cover = 0; cover <= limit; cover++)
            climate[cover] = WorldClimate.getCoveredTemp(level, temperaturePosition, cover);
        return climate;
    }

    private static boolean climateMoved(float[] previous, float[] next) {
        if (previous.length != next.length) return true;
        for (int index = 0; index < next.length; index++)
            if (Math.abs(previous[index] - next[index]) >= CLIMATE_DELTA_C) return true;
        return false;
    }

    /** A block in this column changed opacity, so the cover below it may differ. */
    public void markCoverColumn(int worldX, int worldZ) {
        dirtyCoverColumns.add((long) worldX << 32 | worldZ & 0xffff_ffffL);
    }

    private void refreshCoverColumn(LevelChunk chunk, int worldX, int worldZ) {
        ClimateCoverColumns columns = ((ClimateCoverChunk) chunk).frostedheart$getClimateCover();
        int limit = ClimateCover.limit(), column = (worldX & 15) | (worldZ & 15) << 4;
        for (int sectionY = chunk.getMinSection(); sectionY < chunk.getMaxSection(); sectionY++) {
            Entry entry = entries.get(SectionPos.asLong(chunk.getPos().x, sectionY, chunk.getPos().z));
            if (entry == null || entry.profile == null) continue;
            byte[] cover = null;
            for (int y = 0; y < 16; y++) {
                int index = column | y << 8;
                int value = columns.coverAbove(chunk, worldX, SectionPos.sectionToBlockCoord(sectionY) + y, worldZ, limit);
                if (Byte.toUnsignedInt(entry.profile.cover()[index]) == value) continue;
                if (cover == null) cover = entry.profile.cover().clone();
                cover[index] = (byte) value;
            }
            if (cover == null) continue;
            entry.profile = entry.profile.withCover(cover);
            profiles.put(entry.handle.sectionKey(), entry.profile);
        }
    }

    void captureNewBricks(
            ThermalPageHandle handle,
            LevelChunk chunk,
            long addedBrickMask
    ) {
        Entry entry = entries.get(handle.sectionKey());
        if (entry == null || entry.handle != handle) {
            return;
        }
        long topBricks = (addedBrickMask | addedBrickMask >>> 16 | addedBrickMask >>> 32 | addedBrickMask >>> 48) & 0xffffL;
        int minX = SectionPos.sectionToBlockCoord(
                SectionPos.x(handle.sectionKey()));
        int minY = SectionPos.sectionToBlockCoord(
                SectionPos.y(handle.sectionKey()));
        int minZ = SectionPos.sectionToBlockCoord(
                SectionPos.z(handle.sectionKey()));
        while (topBricks != 0L) {
            int brick = Long.numberOfTrailingZeros(topBricks);
            int brickMinX = (brick & 3) << 2;
            int brickMinZ = (brick >>> 2 & 3) << 2;
            for (int z = brickMinZ; z < brickMinZ + 4; z++) {
                for (int x = brickMinX; x < brickMinX + 4; x++) {
                    int exposedY = chunk.getHeight(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            minX + x, minZ + z);
                    int localY = Math.max(0, Math.min(16, exposedY - minY));
                    int column = x | z << 4;
                    if (Byte.toUnsignedInt(entry.firstExposedLocalY[column])
                            != localY) {
                        entry.firstExposedLocalY[column] = (byte) localY;
                        accumulator.updateSkyColumn(handle, column, localY);
                    }
                }
            }
            topBricks &= topBricks - 1L;
        }
    }

    void track(
            ThermalPageHandle handle,
            Captured captured,
            long gameTick
    ) {
        Entry entry = new Entry(
                handle,
                captured.naturalTemperatureC,
                captured.firstExposedLocalY.clone(),
                nextRefreshTick(handle.sectionKey(), gameTick));
        entry.profile = captured.profile;
        entries.put(handle.sectionKey(), entry);
        naturalQueue.add(entry);
        if (entry.profile != null) profiles.put(handle.sectionKey(), entry.profile);
    }

    void untrack(ThermalPageHandle handle) {
        Entry entry = entries.get(handle.sectionKey());
        if (entry != null && entry.handle == handle) {
            entries.remove(handle.sectionKey());
            profiles.remove(handle.sectionKey());
        }
    }

    public void replaceAccumulator(DimensionInputAccumulator next) {
        accumulator = next;
        if (Double.isFinite(capturedWindScale)) {
            accumulator.updateFarFieldConductanceScale(
                    capturedWindScale);
        }
    }

    Captured current(ThermalPageHandle handle) {
        Entry entry = entries.get(handle.sectionKey());
        if (entry == null || entry.handle != handle) {
            return null;
        }
        return new Captured(
                entry.naturalTemperatureC,
                entry.firstExposedLocalY.clone(),
                entry.profile);
    }

    void markSkyColumn(int worldX, int worldZ) {
        dirtyColumns.add((long) worldX << 32 | worldZ & 0xffff_ffffL);
    }

    void tick(long gameTick, MinecraftPageManager pages) {
        if (gameTick >= nextWindRefreshTick) {
            nextWindRefreshTick = gameTick + REFRESH_INTERVAL_TICKS;
            double windScale = 1.0D + WIND_GAIN
                    * Math.max(
                            0,
                            Math.min(100, WorldTemperature.wind(level)))
                    / 100.0D;
            if (Double.isNaN(capturedWindScale)
                    || Math.abs(capturedWindScale - windScale)
                            >= WIND_SCALE_DELTA) {
                capturedWindScale = windScale;
                accumulator.updateFarFieldConductanceScale(windScale);
            }
        }
        refreshNatural(gameTick);
        int remaining = MAX_SKY_COLUMNS_PER_TICK;
        while (remaining-- > 0 && !dirtyColumns.isEmpty()) {
            long packed = dirtyColumns.removeFirstLong();
            int worldX = (int) (packed >> 32);
            int worldZ = (int) packed;
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                    SectionPos.blockToSectionCoord(worldX),
                    SectionPos.blockToSectionCoord(worldZ));
            if (chunk == null) {
                continue;
            }
            int exposedY = chunk.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    worldX,
                    worldZ);
            pages.refreshSkyColumn(
                    worldX, worldZ, exposedY);
        }
        remaining = MAX_COVER_COLUMNS_PER_TICK;
        while (remaining-- > 0 && !dirtyCoverColumns.isEmpty()) {
            long packed = dirtyCoverColumns.removeFirstLong();
            int worldX = (int) (packed >> 32);
            int worldZ = (int) packed;
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                    SectionPos.blockToSectionCoord(worldX),
                    SectionPos.blockToSectionCoord(worldZ));
            if (chunk != null) refreshCoverColumn(chunk, worldX, worldZ);
        }
    }

    void updateSkyColumn(
            ThermalPageHandle handle,
            int worldX,
            int worldZ,
            int exposedWorldY
    ) {
        Entry entry = entries.get(handle.sectionKey());
        if (entry == null || entry.handle != handle) {
            return;
        }
        int localX = SectionPos.sectionRelative(worldX);
        int localZ = SectionPos.sectionRelative(worldZ);
        int column = localX | localZ << 4;
        int localY = Math.max(
                0,
                Math.min(
                        16,
                        exposedWorldY - SectionPos.sectionToBlockCoord(
                                SectionPos.y(handle.sectionKey()))));
        if (Byte.toUnsignedInt(entry.firstExposedLocalY[column]) != localY) {
            entry.firstExposedLocalY[column] = (byte) localY;
            accumulator.updateSkyColumn(handle, column, localY);
        }
    }

    private void refreshNatural(long gameTick) {
        int remaining = MAX_NATURAL_REFRESHES_PER_TICK;
        while (remaining-- > 0
                && !naturalQueue.isEmpty()
                && naturalQueue.peek().nextRefreshTick <= gameTick) {
            Entry entry = naturalQueue.poll();
            if (entries.get(entry.handle.sectionKey()) != entry) {
                continue;
            }
            double natural = naturalTemperature(entry.handle.sectionKey());
            if (Math.abs(natural - entry.naturalTemperatureC)
                    >= NATURAL_DELTA_C) {
                entry.naturalTemperatureC = natural;
                accumulator.updateNaturalTemperature(entry.handle, natural);
            }
            if (entry.profile != null) {
                float[] climate = coveredClimate(entry.handle.sectionKey(), ClimateCover.limit());
                if (climateMoved(entry.profile.coveredClimate(), climate)) {
                    entry.profile = entry.profile.withClimate(climate);
                    profiles.put(entry.handle.sectionKey(), entry.profile);
                }
            }
            entry.nextRefreshTick = gameTick + REFRESH_INTERVAL_TICKS;
            naturalQueue.add(entry);
        }
    }

    private double naturalTemperature(long sectionKey) {
        temperaturePosition.set(
                SectionPos.sectionToBlockCoord(SectionPos.x(sectionKey)) + 8,
                SectionPos.sectionToBlockCoord(SectionPos.y(sectionKey)) + 8,
                SectionPos.sectionToBlockCoord(SectionPos.z(sectionKey)) + 8);
        return WorldTemperature.naturalAir(level, temperaturePosition);
    }

    private static long nextRefreshTick(long sectionKey, long gameTick) {
        long offset = Math.floorMod(
                sectionKey ^ sectionKey >>> 32,
                REFRESH_INTERVAL_TICKS);
        long interval = Math.floorDiv(gameTick, REFRESH_INTERVAL_TICKS)
                * REFRESH_INTERVAL_TICKS;
        long next = interval + offset;
        return next <= gameTick ? next + REFRESH_INTERVAL_TICKS : next;
    }

    @Override
    public void close() {
        entries.clear();
        naturalQueue.clear();
        dirtyColumns.clear();
        dirtyCoverColumns.clear();
        profiles.clear();
    }

    record Captured(
            double naturalTemperatureC,
            byte[] firstExposedLocalY,
            NaturalProfile profile
    ) {
    }

    private static final class Entry {
        private final ThermalPageHandle handle;
        private double naturalTemperatureC;
        private final byte[] firstExposedLocalY;
        private long nextRefreshTick;
        private NaturalProfile profile;

        private Entry(
                ThermalPageHandle handle,
                double naturalTemperatureC,
                byte[] firstExposedLocalY,
                long nextRefreshTick
        ) {
            this.handle = handle;
            this.naturalTemperatureC = naturalTemperatureC;
            this.firstExposedLocalY = firstExposedLocalY;
            this.nextRefreshTick = nextRefreshTick;
        }
    }
}
