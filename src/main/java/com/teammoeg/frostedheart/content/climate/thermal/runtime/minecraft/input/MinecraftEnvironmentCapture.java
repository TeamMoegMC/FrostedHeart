/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;

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

/** Bounded main-thread natural-temperature and sky-column capture. */
public final class MinecraftEnvironmentCapture implements AutoCloseable {
    private static final long REFRESH_INTERVAL_TICKS = 200L;
    private static final int MAX_NATURAL_REFRESHES_PER_TICK = 16;
    private static final int MAX_SKY_COLUMNS_PER_TICK = 64;
    private static final double NATURAL_DELTA_C = 0.25D;
    private static final double WIND_GAIN = 0.8D;
    private static final double WIND_SCALE_DELTA = 0.05D;

    private final ServerLevel level;
    private DimensionInputAccumulator accumulator;
    private final Long2ObjectOpenHashMap<Entry> entries =
            new Long2ObjectOpenHashMap<>();
    private final PriorityQueue<Entry> naturalQueue =
            new PriorityQueue<>(Comparator
                    .comparingLong((Entry entry) -> entry.nextRefreshTick)
                    .thenComparingLong(entry -> entry.handle.sectionKey()));
    private final LongLinkedOpenHashSet dirtyColumns =
            new LongLinkedOpenHashSet();
    private final BlockPos.MutableBlockPos temperaturePosition =
            new BlockPos.MutableBlockPos();
    private long nextWindRefreshTick;
    private double capturedWindScale = Double.NaN;

    public MinecraftEnvironmentCapture(
            ServerLevel level,
            DimensionInputAccumulator accumulator
    ) {
        this.level = level;
        this.accumulator = accumulator;
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
        long topBricks = residentBrickMask & 0xffff_0000_0000_0000L;
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
        return new Captured(natural, sky);
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
        long topBricks = addedBrickMask & 0xffff_0000_0000_0000L;
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
        entries.put(handle.sectionKey(), entry);
        naturalQueue.add(entry);
    }

    void untrack(ThermalPageHandle handle) {
        Entry entry = entries.get(handle.sectionKey());
        if (entry != null && entry.handle == handle) {
            entries.remove(handle.sectionKey());
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
                entry.firstExposedLocalY.clone());
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
            entry.nextRefreshTick += REFRESH_INTERVAL_TICKS;
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
    }

    record Captured(
            double naturalTemperatureC,
            byte[] firstExposedLocalY
    ) {
    }

    private static final class Entry {
        private final ThermalPageHandle handle;
        private double naturalTemperatureC;
        private final byte[] firstExposedLocalY;
        private long nextRefreshTick;

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
