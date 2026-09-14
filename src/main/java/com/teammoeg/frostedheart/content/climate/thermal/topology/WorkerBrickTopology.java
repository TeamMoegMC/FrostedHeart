/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout;

final class WorkerBrickTopology {
    static final WorkerBrickTopology EMPTY = new WorkerBrickTopology(ArenaSpan.EMPTY, -1, 0, null, 0,
            new int[0], false, false);
    final ArenaSpan span;
    final int coverageSlot, coverageGeneration, transportNodeCount;
    final BlockBrickLayout blockLayout;
    final int[] phaseSlots;
    final boolean cellsResolved, resolved;
    WorkerBrickTopology(ArenaSpan span, int coverageSlot, int coverageGeneration,
            BlockBrickLayout blockLayout, int transportNodeCount,
            int[] phaseSlots,
            boolean cellsResolved, boolean resolved) {
        this.span=span; this.coverageSlot=coverageSlot; this.coverageGeneration=coverageGeneration;
        this.blockLayout=blockLayout; this.transportNodeCount=transportNodeCount;
        this.phaseSlots=phaseSlots;
        this.cellsResolved=cellsResolved; this.resolved=resolved;
    }
    WorkerBrickTopology withFragmentResult(boolean resolved) {
        return new WorkerBrickTopology(span, coverageSlot, coverageGeneration, blockLayout,
                transportNodeCount, phaseSlots, cellsResolved, resolved);
    }
    WorkerBrickTopology withLayout(BlockBrickLayout layout) {
        return new WorkerBrickTopology(span, coverageSlot, coverageGeneration, layout, transportNodeCount,
                phaseSlots, cellsResolved, resolved);
    }
    int nodeAt(int block) { return blockLayout == null ? (coverageSlot < 0 ? -1 : 0) : blockLayout.nodeAt(block); }
    int slotAt(int block) { int n=nodeAt(block); return n < 0 ? -1 : span.firstSlot()+n; }
    int transportSlot(int block) { int n=nodeAt(block); return n < 0 || n >= transportNodeCount ? -1 : span.firstSlot()+n; }
    long nodeMask(int node) { return blockLayout == null ? -1L : blockLayout.nodeBlockMask(node); }
}
