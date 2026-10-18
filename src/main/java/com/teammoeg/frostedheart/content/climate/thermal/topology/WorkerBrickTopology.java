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
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout;

final class WorkerBrickTopology {
    static final WorkerBrickTopology EMPTY =
            new WorkerBrickTopology(ArenaSpan.EMPTY, -1, 0, null, 0, new int[0], false, false);
    final ArenaSpan span;
    final int coverageSlot, coverageGeneration, airNodeCount;
    final BlockBrickLayout blockLayout;
    final int[] phaseSlots;
    final boolean cellsResolved, resolved;

    WorkerBrickTopology(
            ArenaSpan span,
            int coverageSlot,
            int coverageGeneration,
            BlockBrickLayout blockLayout,
            int airNodeCount,
            int[] phaseSlots,
            boolean cellsResolved,
            boolean resolved) {
        this.span = span;
        this.coverageSlot = coverageSlot;
        this.coverageGeneration = coverageGeneration;
        this.blockLayout = blockLayout;
        this.airNodeCount = airNodeCount;
        this.phaseSlots = phaseSlots;
        this.cellsResolved = cellsResolved;
        this.resolved = resolved;
    }

    WorkerBrickTopology withFragmentResult(boolean resolved) {
        return new WorkerBrickTopology(
                span,
                coverageSlot,
                coverageGeneration,
                blockLayout,
                airNodeCount,
                phaseSlots,
                cellsResolved,
                resolved);
    }

    WorkerBrickTopology withLayout(BlockBrickLayout layout) {
        return new WorkerBrickTopology(
                span,
                coverageSlot,
                coverageGeneration,
                layout,
                airNodeCount,
                phaseSlots,
                cellsResolved,
                resolved);
    }

    int nodeAt(int block) {
        return blockLayout == null ? (coverageSlot < 0 ? -1 : 0) : blockLayout.nodeAt(block);
    }

    int slotAt(int block) {
        int n = nodeAt(block);
        return n < 0 ? -1 : span.firstSlot() + n;
    }

    int airSlotAt(int block) {
        int n = nodeAt(block);
        return n < 0 || n >= airNodeCount ? -1 : span.firstSlot() + n;
    }

    long nodeMask(int node) {
        return blockLayout == null ? -1L : blockLayout.nodeBlockMask(node);
    }
}
