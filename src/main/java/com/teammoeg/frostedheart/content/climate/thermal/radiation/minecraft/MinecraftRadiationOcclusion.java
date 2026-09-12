/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.radiation.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.radiation.RadiationService;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.RadiationService.MutableTrace;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.RadiationService.TraceStatus;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPageManager;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Objects;

/** Loaded-only block-grid DDA and physical-source radiation revision table. */
public final class MinecraftRadiationOcclusion implements RadiationService.OcclusionTracer {
    private static final double TIE_EPSILON = 1.0e-12D;

    private final ServerLevel level;
    private final MinecraftPageManager pages;
    private final int maximumTrackedSections;
    private final Long2LongOpenHashMap sectionRevisions =
            new Long2LongOpenHashMap();
    private final LongOpenHashSet loadedSections = new LongOpenHashSet();

    private long nextSectionRevision;
    private LevelChunkSection traceSection;

    public MinecraftRadiationOcclusion(
            ServerLevel level,
            MinecraftPageManager pages,
            int maximumTrackedSections
    ) {
        this.level = Objects.requireNonNull(level, "level");
        this.pages = Objects.requireNonNull(pages, "pages");
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
            boolean collectWitnesses,
            MutableTrace result
    ) {
        try {
            traceBlocks(
                    sourceX, sourceY, sourceZ,
                    targetX, targetY, targetZ,
                    maximumSteps, collectWitnesses, result);
        } finally {
            traceSection = null;
        }
    }

    private void traceBlocks(
            double sourceX,
            double sourceY,
            double sourceZ,
            double targetX,
            double targetY,
            double targetZ,
            int maximumSteps,
            boolean collectWitnesses,
            MutableTrace result
    ) {
        double deltaX = targetX - sourceX;
        double deltaY = targetY - sourceY;
        double deltaZ = targetZ - sourceZ;
        int blockX = floorInt(sourceX);
        int blockY = floorInt(sourceY);
        int blockZ = floorInt(sourceZ);
        int targetBlockX = floorInt(targetX);
        int targetBlockY = floorInt(targetY);
        int targetBlockZ = floorInt(targetZ);
        int stepX = Integer.signum(Double.compare(deltaX, 0.0D));
        int stepY = Integer.signum(Double.compare(deltaY, 0.0D));
        int stepZ = Integer.signum(Double.compare(deltaZ, 0.0D));
        double tDeltaX = stepX == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0D / deltaX);
        double tDeltaY = stepY == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0D / deltaY);
        double tDeltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0D / deltaZ);
        double tMaxX = initialTMax(sourceX, deltaX, stepX);
        double tMaxY = initialTMax(sourceY, deltaY, stepY);
        double tMaxZ = initialTMax(sourceZ, deltaZ, stepZ);

        int currentSectionX = blockX >> 4;
        int currentSectionY = blockY >> 4;
        int currentSectionZ = blockZ >> 4;
        long currentSection = RadiationService.packSection(
                currentSectionX, currentSectionY, currentSectionZ);
        TraceStatus sectionFailure = enterSection(
                currentSection, collectWitnesses, result);
        if (sectionFailure != null) {
            result.finish(sectionFailure);
            return;
        }
        if (blockX == targetBlockX
                && blockY == targetBlockY
                && blockZ == targetBlockZ) {
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
                blockX += stepX;
                tMaxX += tDeltaX;
            }
            if (tMaxY <= next + TIE_EPSILON) {
                blockY += stepY;
                tMaxY += tDeltaY;
            }
            if (tMaxZ <= next + TIE_EPSILON) {
                blockZ += stepZ;
                tMaxZ += tDeltaZ;
            }
            int nextSectionX = blockX >> 4;
            int nextSectionY = blockY >> 4;
            int nextSectionZ = blockZ >> 4;
            if (nextSectionX != currentSectionX
                    || nextSectionY != currentSectionY
                    || nextSectionZ != currentSectionZ) {
                currentSectionX = nextSectionX;
                currentSectionY = nextSectionY;
                currentSectionZ = nextSectionZ;
                currentSection = RadiationService.packSection(
                        currentSectionX, currentSectionY, currentSectionZ);
                sectionFailure = enterSection(
                        currentSection, collectWitnesses, result);
                if (sectionFailure != null) {
                    result.finish(sectionFailure);
                    return;
                }
            }
            BlockStatus status = blockStatus(blockX, blockY, blockZ);
            if (status == BlockStatus.OCCLUDED) {
                result.finish(TraceStatus.BLOCKED);
                return;
            }
            if (status == BlockStatus.UNRESOLVED) {
                result.finish(TraceStatus.UNRESOLVED);
                return;
            }
            if (blockX == targetBlockX && blockY == targetBlockY
                    && blockZ == targetBlockZ) {
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

    public synchronized void onSectionMutation(int sectionX, int sectionY, int sectionZ) {
        long key = RadiationService.packSection(sectionX, sectionY, sectionZ);
        if (sectionRevisions.containsKey(key)) {
            nextSectionRevision = Math.incrementExact(nextSectionRevision);
            sectionRevisions.put(key, nextSectionRevision);
        }
    }

    public synchronized void onChunkLoad(LevelChunk chunk) {
        updateChunkState(chunk, true);
    }

    public synchronized void onChunkUnload(LevelChunk chunk) {
        updateChunkState(chunk, false);
    }

    public synchronized void onSectionIdentityReplaced(int sectionX, int sectionY, int sectionZ) {
        long key = RadiationService.packSection(sectionX, sectionY, sectionZ);
        if (sectionRevisions.containsKey(key)) {
            nextSectionRevision = Math.incrementExact(nextSectionRevision);
            sectionRevisions.put(key, nextSectionRevision);
            loadedSections.add(key);
        }
    }

    private TraceStatus enterSection(
            long sectionKey,
            boolean collectWitnesses,
            MutableTrace result
    ) {
        if (collectWitnesses) {
            long revision = ensureSection(sectionKey);
            if (revision == RadiationService.NO_SECTION_REVISION
                    || !result.addSection(sectionKey, revision)) {
                return TraceStatus.BUDGET_LIMITED;
            }
            return traceSection == null ? TraceStatus.UNRESOLVED : null;
        }
        LevelChunkSection section = loadedSection(sectionKey);
        if (section == null) {
            return TraceStatus.UNRESOLVED;
        }
        traceSection = section;
        return null;
    }

    private synchronized long ensureSection(long sectionKey) {
        long revision = sectionRevisions.get(sectionKey);
        if (revision == RadiationService.NO_SECTION_REVISION
                && sectionRevisions.size() >= maximumTrackedSections) {
            return RadiationService.NO_SECTION_REVISION;
        }
        MinecraftPageManager.SectionOwner owner =
                pages.loadedSectionOrAttach(sectionKey);
        boolean loaded = owner != null;
        traceSection = loaded ? owner.section() : null;
        if (revision == RadiationService.NO_SECTION_REVISION) {
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

    private LevelChunkSection loadedSection(long sectionKey) {
        int sectionX = RadiationService.sectionX(sectionKey);
        int sectionY = RadiationService.sectionY(sectionKey);
        int sectionZ = RadiationService.sectionZ(sectionKey);
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                sectionX, sectionZ);
        if (chunk == null) {
            return null;
        }
        int index = chunk.getSectionIndexFromSectionY(sectionY);
        return index >= 0 && index < chunk.getSections().length
                ? chunk.getSections()[index] : null;
    }

    private BlockStatus blockStatus(int blockX, int blockY, int blockZ) {
        if (level.isOutsideBuildHeight(blockY)) {
            return BlockStatus.UNRESOLVED;
        }
        if (traceSection == null) {
            return BlockStatus.UNRESOLVED;
        }
        BlockState state = traceSection.getBlockState(
                blockX & 15,
                blockY & 15,
                blockZ & 15);
        return blocksRadiation(state)
                ? BlockStatus.OCCLUDED : BlockStatus.TRANSPARENT;
    }

    private void updateChunkState(LevelChunk chunk, boolean loaded) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        for (int index = 0; index < chunk.getSections().length; index++) {
            long key = RadiationService.packSection(
                    chunkX,
                    chunk.getSectionYFromSectionIndex(index),
                    chunkZ);
            if (!sectionRevisions.containsKey(key)) {
                continue;
            }
            if (!loaded) {
                sectionRevisions.remove(key);
                loadedSections.remove(key);
                continue;
            }
            if (!loadedSections.add(key)) {
                continue;
            }
            nextSectionRevision = Math.incrementExact(nextSectionRevision);
            sectionRevisions.put(key, nextSectionRevision);
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

    public static boolean blocksRadiation(BlockState state) {
        return !state.getBlock().hasDynamicShape() && state.canOcclude();
    }

    private enum BlockStatus {
        TRANSPARENT,
        OCCLUDED,
        UNRESOLVED
    }
}
