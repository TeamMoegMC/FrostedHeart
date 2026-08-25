/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.radiation.RadiationService;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.RadiationService.MutableTrace;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.RadiationService.TraceStatus;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Objects;

/** Loaded-only quarter-block DDA and independent radiation revision table. */
final class MinecraftRadiationOcclusion implements RadiationService.OcclusionTracer {
    private static final double TIE_EPSILON = 1.0e-12D;

    private final MinecraftThermalInput input;
    private final ServerLevel level;
    private final int maximumTrackedSections;
    private final Long2LongOpenHashMap sectionRevisions =
            new Long2LongOpenHashMap();
    private final LongOpenHashSet loadedSections = new LongOpenHashSet();

    private long nextSectionRevision;
    private long cachedBlockPosition = Long.MIN_VALUE;
    private BlockStatus cachedBlockStatus = BlockStatus.UNRESOLVED;

    MinecraftRadiationOcclusion(
            MinecraftThermalInput input,
            ServerLevel level,
            int maximumTrackedSections
    ) {
        this.input = Objects.requireNonNull(input, "input");
        this.level = Objects.requireNonNull(level, "level");
        if (maximumTrackedSections <= 0) {
            throw new IllegalArgumentException("maximumTrackedSections must be positive");
        }
        this.maximumTrackedSections = maximumTrackedSections;
        sectionRevisions.defaultReturnValue(RadiationService.NO_SECTION_REVISION);
    }

    @Override
    public void trace(
            double sourceX,
            double sourceY,
            double sourceZ,
            double targetX,
            double targetY,
            double targetZ,
            int maximumSteps,
            MutableTrace result
    ) {
        cachedBlockPosition = Long.MIN_VALUE;
        double startX = sourceX * 4.0D;
        double startY = sourceY * 4.0D;
        double startZ = sourceZ * 4.0D;
        double deltaX = targetX * 4.0D - startX;
        double deltaY = targetY * 4.0D - startY;
        double deltaZ = targetZ * 4.0D - startZ;
        int microX = floorInt(startX);
        int microY = floorInt(startY);
        int microZ = floorInt(startZ);
        int targetMicroX = floorInt(targetX * 4.0D);
        int targetMicroY = floorInt(targetY * 4.0D);
        int targetMicroZ = floorInt(targetZ * 4.0D);
        int stepX = Integer.signum(Double.compare(deltaX, 0.0D));
        int stepY = Integer.signum(Double.compare(deltaY, 0.0D));
        int stepZ = Integer.signum(Double.compare(deltaZ, 0.0D));
        double tDeltaX = stepX == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0D / deltaX);
        double tDeltaY = stepY == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0D / deltaY);
        double tDeltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0D / deltaZ);
        double tMaxX = initialTMax(startX, deltaX, stepX);
        double tMaxY = initialTMax(startY, deltaY, stepY);
        double tMaxZ = initialTMax(startZ, deltaZ, stepZ);

        long currentSection = sectionKey(microX, microY, microZ);
        TraceStatus sectionFailure = addSectionWitness(currentSection, result);
        if (sectionFailure != null) {
            result.finish(sectionFailure);
            return;
        }
        if (microX == targetMicroX && microY == targetMicroY && microZ == targetMicroZ) {
            result.finish(TraceStatus.VISIBLE);
            return;
        }

        for (int steps = 0; steps < maximumSteps; steps++) {
            double next = Math.min(tMaxX, Math.min(tMaxY, tMaxZ));
            if (!Double.isFinite(next)) {
                result.finish(TraceStatus.VISIBLE);
                return;
            }
            if (tMaxX <= next + TIE_EPSILON) {
                microX += stepX;
                tMaxX += tDeltaX;
            }
            if (tMaxY <= next + TIE_EPSILON) {
                microY += stepY;
                tMaxY += tDeltaY;
            }
            if (tMaxZ <= next + TIE_EPSILON) {
                microZ += stepZ;
                tMaxZ += tDeltaZ;
            }
            long nextSection = sectionKey(microX, microY, microZ);
            if (nextSection != currentSection) {
                currentSection = nextSection;
                sectionFailure = addSectionWitness(currentSection, result);
                if (sectionFailure != null) {
                    result.finish(sectionFailure);
                    return;
                }
            }
            BlockStatus status = microcellStatus(microX, microY, microZ);
            if (status == BlockStatus.OCCLUDED) {
                result.finish(TraceStatus.BLOCKED);
                return;
            }
            if (status == BlockStatus.UNRESOLVED) {
                result.finish(TraceStatus.UNRESOLVED);
                return;
            }
            if (microX == targetMicroX && microY == targetMicroY
                    && microZ == targetMicroZ) {
                result.finish(TraceStatus.VISIBLE);
                return;
            }
        }
        result.finish(TraceStatus.BUDGET_LIMITED);
    }

    @Override
    public synchronized long currentSectionRevision(long packedSectionKey) {
        return sectionRevisions.get(packedSectionKey);
    }

    synchronized void onSectionMutation(int sectionX, int sectionY, int sectionZ) {
        long key = RadiationService.packSection(sectionX, sectionY, sectionZ);
        if (sectionRevisions.containsKey(key)) {
            nextSectionRevision = Math.incrementExact(nextSectionRevision);
            sectionRevisions.put(key, nextSectionRevision);
        }
    }

    synchronized void onChunkLoad(LevelChunk chunk) {
        updateChunkState(chunk.getPos().x, chunk.getPos().z, true);
    }

    synchronized void onChunkUnload(LevelChunk chunk) {
        updateChunkState(chunk.getPos().x, chunk.getPos().z, false);
    }

    synchronized void onSectionIdentityReplaced(int sectionX, int sectionY, int sectionZ) {
        long key = RadiationService.packSection(sectionX, sectionY, sectionZ);
        if (sectionRevisions.containsKey(key)) {
            nextSectionRevision = Math.incrementExact(nextSectionRevision);
            sectionRevisions.put(key, nextSectionRevision);
            loadedSections.add(key);
        }
    }

    private TraceStatus addSectionWitness(long sectionKey, MutableTrace result) {
        long revision = ensureSection(sectionKey);
        if (revision == RadiationService.NO_SECTION_REVISION
                || !result.addSection(sectionKey, revision)) {
            return TraceStatus.BUDGET_LIMITED;
        }
        input.retainRadiationSection(sectionKey);
        return isRecordedLoaded(sectionKey) ? null : TraceStatus.UNRESOLVED;
    }

    private static long sectionKey(int microX, int microY, int microZ) {
        int blockX = Math.floorDiv(microX, 4);
        int blockY = Math.floorDiv(microY, 4);
        int blockZ = Math.floorDiv(microZ, 4);
        return RadiationService.packSection(
                Math.floorDiv(blockX, 16),
                Math.floorDiv(blockY, 16),
                Math.floorDiv(blockZ, 16));
    }

    private synchronized long ensureSection(long sectionKey) {
        boolean loaded = isLoaded(sectionKey);
        long revision = sectionRevisions.get(sectionKey);
        if (revision == RadiationService.NO_SECTION_REVISION) {
            if (sectionRevisions.size() >= maximumTrackedSections) {
                return RadiationService.NO_SECTION_REVISION;
            }
            nextSectionRevision = Math.incrementExact(nextSectionRevision);
            revision = nextSectionRevision;
            sectionRevisions.put(sectionKey, revision);
            if (loaded) {
                loadedSections.add(sectionKey);
            }
            return revision;
        }
        boolean recordedLoaded = loadedSections.contains(sectionKey);
        if (loaded != recordedLoaded) {
            nextSectionRevision = Math.incrementExact(nextSectionRevision);
            revision = nextSectionRevision;
            sectionRevisions.put(sectionKey, revision);
            if (loaded) {
                loadedSections.add(sectionKey);
            } else {
                loadedSections.remove(sectionKey);
            }
        }
        return revision;
    }

    private synchronized boolean isRecordedLoaded(long sectionKey) {
        return loadedSections.contains(sectionKey);
    }

    private boolean isLoaded(long sectionKey) {
        int sectionX = RadiationService.sectionX(sectionKey);
        int sectionY = RadiationService.sectionY(sectionKey);
        int sectionZ = RadiationService.sectionZ(sectionKey);
        LevelChunk chunk = level.getChunkSource().getChunkNow(sectionX, sectionZ);
        if (chunk == null) {
            return false;
        }
        int index = chunk.getSectionIndexFromSectionY(sectionY);
        return index >= 0 && index < chunk.getSections().length;
    }

    private BlockStatus microcellStatus(int microX, int microY, int microZ) {
        int blockX = Math.floorDiv(microX, 4);
        int blockY = Math.floorDiv(microY, 4);
        int blockZ = Math.floorDiv(microZ, 4);
        if (level.isOutsideBuildHeight(blockY)) {
            return BlockStatus.UNRESOLVED;
        }
        long blockPosition = net.minecraft.core.BlockPos.asLong(blockX, blockY, blockZ);
        if (cachedBlockPosition != blockPosition) {
            cachedBlockPosition = blockPosition;
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                    Math.floorDiv(blockX, 16), Math.floorDiv(blockZ, 16));
            int sectionY = Math.floorDiv(blockY, 16);
            if (chunk == null) {
                cachedBlockStatus = BlockStatus.UNRESOLVED;
            } else {
                int sectionIndex = chunk.getSectionIndexFromSectionY(sectionY);
                if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
                    cachedBlockStatus = BlockStatus.UNRESOLVED;
                } else {
                    LevelChunkSection section = chunk.getSections()[sectionIndex];
                    BlockState state = section.getBlockState(
                            Math.floorMod(blockX, 16),
                            Math.floorMod(blockY, 16),
                            Math.floorMod(blockZ, 16));
                    cachedBlockStatus = state.getBlock().hasDynamicShape()
                            || !state.canOcclude()
                            ? BlockStatus.TRANSPARENT
                            : BlockStatus.OCCLUDED;
                }
            }
        }
        return cachedBlockStatus;
    }

    private synchronized void updateChunkState(int chunkX, int chunkZ, boolean loaded) {
        LongIterator iterator = sectionRevisions.keySet().iterator();
        while (iterator.hasNext()) {
            long key = iterator.nextLong();
            if (RadiationService.sectionX(key) != chunkX
                    || RadiationService.sectionZ(key) != chunkZ) {
                continue;
            }
            if (!loaded) {
                iterator.remove();
                loadedSections.remove(key);
                continue;
            }
            boolean recordedLoaded = loadedSections.contains(key);
            if (recordedLoaded) {
                continue;
            }
            nextSectionRevision = Math.incrementExact(nextSectionRevision);
            sectionRevisions.put(key, nextSectionRevision);
            loadedSections.add(key);
        }
    }

    private static int floorInt(double value) {
        double floor = Math.floor(value);
        if (floor < Integer.MIN_VALUE || floor > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("radiation ray coordinate is out of range");
        }
        return (int) floor;
    }

    private static double initialTMax(double coordinate, double delta, int step) {
        if (step == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return step > 0
                ? (Math.floor(coordinate) + 1.0D - coordinate) / delta
                : (coordinate - Math.floor(coordinate)) / -delta;
    }

    private enum BlockStatus {
        TRANSPARENT,
        OCCLUDED,
        UNRESOLVED
    }
}
