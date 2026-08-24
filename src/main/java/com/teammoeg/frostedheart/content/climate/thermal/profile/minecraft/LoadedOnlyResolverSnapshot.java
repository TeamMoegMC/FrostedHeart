/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.profile.DependencyOffsetMask;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolverBlockView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Main-thread bridge from loaded Minecraft chunks to an immutable resolver
 * snapshot. It uses {@code getChunkNow} exclusively and therefore never loads
 * a missing dependency chunk.
 */
public final class LoadedOnlyResolverSnapshot {
    private LoadedOnlyResolverSnapshot() {
    }

    public static Capture capture(
            ServerLevel level,
            BlockPos center,
            DependencyOffsetMask dependencyMask
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(dependencyMask, "dependencyMask");

        Map<DependencyOffsetMask.Offset,
                ResolverBlockView.SnapshotCell<BlockState, FluidState>> cells =
                new LinkedHashMap<>();
        int present = 0;
        int unloaded = 0;
        int missing = 0;
        for (DependencyOffsetMask.Offset offset : dependencyMask.offsets()) {
            BlockPos position = center.offset(offset.x(), offset.y(), offset.z());
            ResolverBlockView.SnapshotCell<BlockState, FluidState> cell;
            if (level.isOutsideBuildHeight(position)) {
                cell = ResolverBlockView.SnapshotCell.missing();
                missing++;
            } else {
                LevelChunk chunk = level.getChunkSource().getChunkNow(
                        SectionPos.blockToSectionCoord(position.getX()),
                        SectionPos.blockToSectionCoord(position.getZ()));
                if (chunk == null) {
                    cell = ResolverBlockView.SnapshotCell.unloaded();
                    unloaded++;
                } else {
                    BlockState state = chunk.getBlockState(position);
                    cell = ResolverBlockView.SnapshotCell.present(state, state.getFluidState());
                    present++;
                }
            }
            cells.put(offset, cell);
        }

        ResolverBlockView<BlockState, FluidState> view =
                ResolverBlockView.snapshot(dependencyMask, cells);
        return new Capture(view, present, unloaded, missing);
    }

    /** Capture diagnostics contain counts only and retain no World or chunk. */
    public record Capture(
            ResolverBlockView<BlockState, FluidState> view,
            int presentCellCount,
            int unloadedCellCount,
            int missingCellCount
    ) {
        public Capture {
            Objects.requireNonNull(view, "view");
            if (presentCellCount < 0 || unloadedCellCount < 0 || missingCellCount < 0) {
                throw new IllegalArgumentException("capture counts must be non-negative");
            }
            int captured = presentCellCount + unloadedCellCount + missingCellCount;
            if (captured != view.dependencyMask().offsetCount()
                    || presentCellCount != view.presentCellCount()) {
                throw new IllegalArgumentException("capture counts must match the resolver view");
            }
        }

        public boolean isComplete() {
            return unloadedCellCount == 0 && missingCellCount == 0;
        }
    }
}
