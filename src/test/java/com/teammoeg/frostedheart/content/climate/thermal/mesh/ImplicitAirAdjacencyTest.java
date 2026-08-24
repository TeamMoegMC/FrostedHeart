/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ComponentBrickCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometryDeltaRing;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometrySummary;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometrySummaryCache;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.solver.InputWatermarks;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SolveEpoch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImplicitAirAdjacencyTest {
    private static final double EPSILON = 1.0e-9D;
    private static final double AIR_CAPACITY_DENSITY = 1.0D;
    private static final double MIXING_W_PER_BLOCK_K = 1.0D;
    private static final double MINIMUM_MIXED_DISTANCE = 0.1D;
    private static final BuoyancyConductance.Parameters NEUTRAL_BUOYANCY =
            new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D);

    @Test
    void finePageCompilesAllInternalFacesWithoutPersistingEdges() {
        ThermalCellArena arena = new ThermalCellArena(0);
        PageFixture page = regularPage(
                arena, 0, 0, 0, 0,
                uniformCells(0, 0, 0, 16, 4), true);

        ImplicitAirAdjacency.CompiledPairs result = compile(
                page.view(), ImplicitAirAdjacency.PositiveNeighbors.none(), arena);

        assertTrue(result.ownerPublicationCurrent());
        assertEquals(144, result.operations().size());
        assertEquals(144.0D * 16.0D,
                result.totalOpenAreaBlocksSquared(), EPSILON);
        assertEquals(0, result.mixedPairCount());
        assertEquals(0, result.unavailablePositivePages());
    }

    @Test
    void negativePageOwnsSixteenToFourCrossPagePairsExactlyOnce() {
        ThermalCellArena arena = new ThermalCellArena(0);
        PageFixture negative = regularPage(
                arena, 0, 0, 0, 0,
                uniformCells(0, 0, 0, 16, 16), true);
        PageFixture positive = regularPage(
                arena, 1, 16, 0, 0,
                uniformCells(16, 0, 0, 16, 4), true);

        ImplicitAirAdjacency.CompiledPairs result = compile(
                negative.view(),
                new ImplicitAirAdjacency.PositiveNeighbors(
                        positive.view(), null, null),
                arena);

        assertEquals(16, result.operations().size());
        assertEquals(256.0D, result.totalOpenAreaBlocksSquared(), EPSILON);
        for (ThermalSweep.PairOperation operation : result.operations()) {
            assertEquals(1.6D, operation.baseConductanceWPerK(), EPSILON);
        }
    }

    @Test
    void coarseEightCellFindsFourFineNeighborsOnOneInternalFace() {
        ThermalCellArena arena = new ThermalCellArena(0);
        PageFixture page = regularPage(
                arena, 0, 0, 0, 0, oneCoarseOctantAndFineRemainder(), true);
        int coarseSlot = page.span().firstSlot();

        ImplicitAirAdjacency.CompiledPairs result = compile(
                page.view(), ImplicitAirAdjacency.PositiveNeighbors.none(), arena);
        List<ThermalSweep.PairOperation> positiveX = result.operations().stream()
                .filter(operation -> operation.cellA() == coarseSlot)
                .filter(operation -> arena.minimumX(operation.cellB()) == 8)
                .toList();

        assertEquals(4, positiveX.size());
        for (ThermalSweep.PairOperation operation : positiveX) {
            assertEquals(16.0D / 6.0D,
                    operation.baseConductanceWPerK(), EPSILON);
        }
    }

    @Test
    void staleOwnerStopsCompilationAndStalePositivePageRemainsFrontier() {
        ThermalCellArena arena = new ThermalCellArena(0);
        PageFixture owner = regularPage(
                arena, 0, 0, 0, 0,
                uniformCells(0, 0, 0, 16, 16), true);
        PageFixture neighbor = regularPage(
                arena, 1, 16, 0, 0,
                uniformCells(16, 0, 0, 16, 16), true);
        neighbor.page().recordGeometryMutation(
                0, 0, 0, 1L, new GeometryDeltaRing(1));

        ImplicitAirAdjacency.CompiledPairs frontier = compile(
                owner.view(),
                new ImplicitAirAdjacency.PositiveNeighbors(
                        neighbor.view(), null, null),
                arena);
        assertTrue(frontier.ownerPublicationCurrent());
        assertEquals(0, frontier.operations().size());
        assertEquals(1, frontier.unavailablePositivePages());

        owner.page().recordGeometryMutation(
                0, 0, 0, 2L, new GeometryDeltaRing(1));
        ImplicitAirAdjacency.CompiledPairs staleOwner = compile(
                owner.view(), ImplicitAirAdjacency.PositiveNeighbors.none(), arena);
        assertFalse(staleOwner.ownerPublicationCurrent());
        assertEquals(0, staleOwner.operations().size());
    }

    @Test
    void regularToMixedUsesFractionalApertureAndCentroidDistance() {
        ThermalCellArena arena = new ThermalCellArena(0);
        PageFixture page = mixedPage(
                arena,
                new MixedAt(1, oneMicrocellPort(
                        ConservativeAirGeometry.Face.NEGATIVE_X, 1, 1)));
        int mixedCell = arena.mixedComponentSlot(page.mixedSupportRefs()[0], 0);

        ImplicitAirAdjacency.CompiledPairs result = compile(
                page.view(), ImplicitAirAdjacency.PositiveNeighbors.none(), arena);
        ThermalSweep.PairOperation pair = result.operations().stream()
                .filter(operation -> operation.cellB() == mixedCell)
                .findFirst()
                .orElseThrow();

        assertEquals(1, result.mixedPairCount());
        assertEquals((1.0D / 16.0D) / 2.125D,
                pair.baseConductanceWPerK(), EPSILON);
    }

    @Test
    void mixedApertureIntersectionFeedsAConservativeSweep() {
        ThermalCellArena arena = new ThermalCellArena(0);
        PageFixture page = mixedPage(
                arena,
                new MixedAt(1, oneMicrocellPort(
                        ConservativeAirGeometry.Face.POSITIVE_X, 1, 1)),
                new MixedAt(2, oneMicrocellPort(
                        ConservativeAirGeometry.Face.NEGATIVE_X, 1, 1)));
        int left = arena.mixedComponentSlot(page.mixedSupportRefs()[0], 0);
        int right = arena.mixedComponentSlot(page.mixedSupportRefs()[1], 0);
        arena.setEnthalpyJ(left, 100.0D);

        ImplicitAirAdjacency.CompiledPairs result = compile(
                page.view(), ImplicitAirAdjacency.PositiveNeighbors.none(), arena);
        ThermalSweep.PairOperation mixedPair = result.operations().stream()
                .filter(operation -> operation.cellA() == left && operation.cellB() == right)
                .findFirst()
                .orElseThrow();
        assertEquals(1, result.mixedPairCount());
        assertEquals(0.25D, mixedPair.baseConductanceWPerK(), EPSILON);

        double before = sumLiveEnthalpy(arena);
        ThermalSweep sweep = new ThermalSweep(
                arena, result.operations(), List.of(), NEUTRAL_BUOYANCY);
        ThermalSweep.Result sweepResult = sweep.apply(
                0.0D,
                new SolveEpoch(0L, 1L, 1L, 1L, InputWatermarks.ZERO));
        assertEquals(before, sumLiveEnthalpy(arena), EPSILON);
        assertEquals(0.0D, sweepResult.conservationResidualJ(), EPSILON);
        assertTrue(arena.enthalpyJ(right) > 0.0D);
    }

    @Test
    void mixedPortsWithDisjointAperturesDoNotCompileAPair() {
        ThermalCellArena arena = new ThermalCellArena(0);
        PageFixture page = mixedPage(
                arena,
                new MixedAt(1, oneMicrocellPort(
                        ConservativeAirGeometry.Face.POSITIVE_X, 1, 1)),
                new MixedAt(2, oneMicrocellPort(
                        ConservativeAirGeometry.Face.NEGATIVE_X, 2, 1)));

        ImplicitAirAdjacency.CompiledPairs result = compile(
                page.view(), ImplicitAirAdjacency.PositiveNeighbors.none(), arena);

        assertEquals(0, result.mixedPairCount());
    }

    @Test
    void pageViewRejectsMisalignmentAndNeighborMustBePositiveAdjacent() {
        ThermalCellArena arena = new ThermalCellArena(0);
        PageFixture owner = regularPage(
                arena, 0, 0, 0, 0,
                uniformCells(0, 0, 0, 16, 16), true);
        PageFixture wrongNeighbor = regularPage(
                arena, 1, 32, 0, 0,
                uniformCells(32, 0, 0, 16, 16), true);

        assertThrows(IllegalArgumentException.class, () ->
                new ImplicitAirAdjacency.PageView(owner.page(), 0, 4, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> compile(
                owner.view(),
                new ImplicitAirAdjacency.PositiveNeighbors(
                        wrongNeighbor.view(), null, null),
                arena));
    }

    @Test
    void publishedCoverageCannotReferenceAnotherPagesArenaSupport() {
        ThermalCellArena arena = new ThermalCellArena(0);
        ArenaSpan ownerCells = arena.allocatePageCells(
                0,
                1,
                uniformCells(0, 0, 0, 16, 4),
                0.0D,
                0.0D);
        ArenaSpan foreign = arena.allocatePageCells(
                1,
                1,
                uniformCells(16, 0, 0, 16, 16),
                0.0D,
                0.0D);
        ThermalPage corrupt = new ThermalPage(300L, 1L);
        assertTrue(corrupt.tryInstallGeometryBuild(
                0L,
                ThermalPage.FullGeometryState.uniformAllAir(
                        foreign.firstSlot(), 0, ownerCells)));
        assertTrue(corrupt.tryPublishGeometry(0L, corrupt.topologyGeneration(), 0L));

        assertThrows(IllegalStateException.class, () -> compile(
                new ImplicitAirAdjacency.PageView(corrupt, 0, 0, 0, 0),
                ImplicitAirAdjacency.PositiveNeighbors.none(),
                arena));
    }

    private static ImplicitAirAdjacency.CompiledPairs compile(
            ImplicitAirAdjacency.PageView owner,
            ImplicitAirAdjacency.PositiveNeighbors neighbors,
            ThermalCellArena arena
    ) {
        return ImplicitAirAdjacency.compileOwnedPairs(
                owner,
                neighbors,
                arena,
                MIXING_W_PER_BLOCK_K,
                MINIMUM_MIXED_DISTANCE,
                false);
    }

    private static PageFixture regularPage(
            ThermalCellArena arena,
            int pageSlot,
            int minX,
            int minY,
            int minZ,
            ThermalCellArena.CellSpec[] cells,
            boolean publish
    ) {
        ArenaSpan span = arena.allocatePageCells(
                pageSlot, 1, cells, 0.0D, 0.0D);
        ThermalPage page = new ThermalPage(100L + pageSlot, 1L);
        assertTrue(page.tryInstallGeometryBuild(
                0L, regularAirState(arena, span, minX, minY, minZ)));
        if (publish) {
            assertTrue(page.tryPublishGeometry(
                    page.liveGeometryRevision(), page.topologyGeneration(), 0L));
        }
        return new PageFixture(
                page,
                span,
                new ImplicitAirAdjacency.PageView(
                        page, pageSlot, minX, minY, minZ),
                new int[0]);
    }

    private static PageFixture mixedPage(
            ThermalCellArena arena,
            MixedAt... mixedBricks
    ) {
        boolean[] mixedAtBase = new boolean[ThermalPage.BASE_BRICK_COUNT];
        ThermalCellArena.MixedBrickSpec[] specs =
                new ThermalCellArena.MixedBrickSpec[mixedBricks.length];
        for (int index = 0; index < mixedBricks.length; index++) {
            MixedAt mixed = mixedBricks[index];
            if (mixedAtBase[mixed.baseIndex()]) {
                throw new IllegalArgumentException("duplicate mixed base index");
            }
            mixedAtBase[mixed.baseIndex()] = true;
            specs[index] = new ThermalCellArena.MixedBrickSpec(
                    baseWorldX(mixed.baseIndex()),
                    baseWorldY(mixed.baseIndex()),
                    baseWorldZ(mixed.baseIndex()),
                    mixed.geometry(),
                    0,
                    0,
                    AIR_CAPACITY_DENSITY);
        }
        List<ThermalCellArena.CellSpec> regular = new ArrayList<>();
        for (int baseIndex = 0; baseIndex < ThermalPage.BASE_BRICK_COUNT; baseIndex++) {
            if (!mixedAtBase[baseIndex]) {
                regular.add(cell(
                        baseWorldX(baseIndex),
                        baseWorldY(baseIndex),
                        baseWorldZ(baseIndex),
                        4));
            }
        }

        ThermalCellArena.PageAllocation allocation = arena.allocatePageCells(
                0,
                1,
                regular.toArray(ThermalCellArena.CellSpec[]::new),
                specs,
                0.0D,
                0.0D);
        int[] mixedSupports = allocation.mixedSupportRefs();
        int[] refs = new int[ThermalPage.BASE_BRICK_COUNT];
        byte[] widths = new byte[ThermalPage.BASE_BRICK_COUNT];
        GeometrySummary[] summaries =
                new GeometrySummary[GeometrySummaryCache.SUMMARY_COUNT];
        Arrays.fill(summaries, GeometrySummary.singleAir(0));
        long mixedMask = 0L;
        for (int baseIndex = 0; baseIndex < ThermalPage.BASE_BRICK_COUNT; baseIndex++) {
            int mixedIndex = mixedIndex(mixedBricks, baseIndex);
            refs[baseIndex] = mixedIndex >= 0
                    ? mixedSupports[mixedIndex]
                    : cellCovering(
                            arena,
                            allocation.cellSpan(),
                            baseWorldX(baseIndex),
                            baseWorldY(baseIndex),
                            baseWorldZ(baseIndex));
            widths[baseIndex] = 4;
            if (mixedIndex >= 0) {
                mixedMask |= 1L << baseIndex;
                summaries[baseIndex] = GeometrySummary.mixed(
                        GeometrySummary.MATERIAL_INTERFACE);
            }
        }
        ThermalPage page = new ThermalPage(100L, 1L);
        assertTrue(page.tryInstallGeometryBuild(0L, new ThermalPage.FullGeometryState(
                refs,
                widths,
                summaries,
                mixedMask,
                allocation.cellSpan())));
        assertTrue(page.tryPublishGeometry(0L, page.topologyGeneration(), 0L));
        return new PageFixture(
                page,
                allocation.cellSpan(),
                new ImplicitAirAdjacency.PageView(page, 0, 0, 0, 0),
                mixedSupports);
    }

    private static ThermalCellArena.CellSpec[] uniformCells(
            int minX,
            int minY,
            int minZ,
            int parentWidth,
            int childWidth
    ) {
        List<ThermalCellArena.CellSpec> cells = new ArrayList<>();
        for (int y = 0; y < parentWidth; y += childWidth) {
            for (int z = 0; z < parentWidth; z += childWidth) {
                for (int x = 0; x < parentWidth; x += childWidth) {
                    cells.add(cell(minX + x, minY + y, minZ + z, childWidth));
                }
            }
        }
        return cells.toArray(ThermalCellArena.CellSpec[]::new);
    }

    private static ThermalCellArena.CellSpec[] oneCoarseOctantAndFineRemainder() {
        List<ThermalCellArena.CellSpec> cells = new ArrayList<>();
        cells.add(cell(0, 0, 0, 8));
        for (int y = 0; y < 16; y += 4) {
            for (int z = 0; z < 16; z += 4) {
                for (int x = 0; x < 16; x += 4) {
                    if (x < 8 && y < 8 && z < 8) {
                        continue;
                    }
                    cells.add(cell(x, y, z, 4));
                }
            }
        }
        return cells.toArray(ThermalCellArena.CellSpec[]::new);
    }

    private static ThermalCellArena.CellSpec cell(
            int minX,
            int minY,
            int minZ,
            int width
    ) {
        return ThermalCellArena.CellSpec.regularAir(
                minX,
                minY,
                minZ,
                width,
                0,
                0,
                AIR_CAPACITY_DENSITY);
    }

    private static ThermalPage.FullGeometryState regularAirState(
            ThermalCellArena arena,
            ArenaSpan span,
            int pageMinX,
            int pageMinY,
            int pageMinZ
    ) {
        int[] refs = new int[ThermalPage.BASE_BRICK_COUNT];
        byte[] widths = new byte[ThermalPage.BASE_BRICK_COUNT];
        for (int baseIndex = 0; baseIndex < ThermalPage.BASE_BRICK_COUNT; baseIndex++) {
            int worldX = pageMinX + baseWorldX(baseIndex);
            int worldY = pageMinY + baseWorldY(baseIndex);
            int worldZ = pageMinZ + baseWorldZ(baseIndex);
            int slot = cellCovering(arena, span, worldX, worldY, worldZ);
            refs[baseIndex] = slot;
            widths[baseIndex] = (byte) arena.widthBlocks(slot);
        }
        GeometrySummary[] summaries =
                new GeometrySummary[GeometrySummaryCache.SUMMARY_COUNT];
        Arrays.fill(summaries, GeometrySummary.singleAir(0));
        return new ThermalPage.FullGeometryState(refs, widths, summaries, 0L, span);
    }

    private static int cellCovering(
            ThermalCellArena arena,
            ArenaSpan span,
            int worldX,
            int worldY,
            int worldZ
    ) {
        for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++) {
            int width = arena.widthBlocks(slot);
            if (arena.isRegularCell(slot)
                    && worldX >= arena.minimumX(slot)
                    && worldX < arena.minimumX(slot) + width
                    && worldY >= arena.minimumY(slot)
                    && worldY < arena.minimumY(slot) + width
                    && worldZ >= arena.minimumZ(slot)
                    && worldZ < arena.minimumZ(slot) + width) {
                return slot;
            }
        }
        throw new AssertionError("missing regular cell coverage");
    }

    private static ComponentBrickCompiler.CompiledBrick oneMicrocellPort(
            ConservativeAirGeometry.Face face,
            int apertureY,
            int apertureZ
    ) {
        List<ConservativeAirGeometry.Resolution> blocks = repeated(solid());
        int blockX = face == ConservativeAirGeometry.Face.NEGATIVE_X ? 0 : 3;
        int microX = face == ConservativeAirGeometry.Face.NEGATIVE_X ? 0 : 3;
        blocks.set(
                ComponentBrickCompiler.blockIndex(blockX, 1, 1),
                singleMicrocell(microX, apertureY, apertureZ));
        ComponentBrickCompiler.Compilation compilation =
                ComponentBrickCompiler.compile(blocks, 4, 0);
        assertEquals(ComponentBrickCompiler.Status.RESOLVED, compilation.status());
        return compilation.brick().orElseThrow();
    }

    private static ConservativeAirGeometry.Resolution singleMicrocell(
            int openX,
            int openY,
            int openZ
    ) {
        List<ConservativeAirGeometry.UnitBox> blockers = new ArrayList<>();
        for (int y = 0; y < 4; y++) {
            for (int z = 0; z < 4; z++) {
                for (int x = 0; x < 4; x++) {
                    if (x != openX || y != openY || z != openZ) {
                        blockers.add(microcell(x, y, z));
                    }
                }
            }
        }
        return ConservativeAirGeometry.resolve(blockers, 1);
    }

    private static ConservativeAirGeometry.UnitBox microcell(int x, int y, int z) {
        double size = 0.25D;
        return new ConservativeAirGeometry.UnitBox(
                x * size,
                y * size,
                z * size,
                (x + 1) * size,
                (y + 1) * size,
                (z + 1) * size);
    }

    private static ConservativeAirGeometry.Resolution solid() {
        return ConservativeAirGeometry.resolve(
                List.of(ConservativeAirGeometry.UnitBox.fullBlock()), 4);
    }

    private static List<ConservativeAirGeometry.Resolution> repeated(
            ConservativeAirGeometry.Resolution resolution
    ) {
        return new ArrayList<>(java.util.Collections.nCopies(64, resolution));
    }

    private static int mixedIndex(MixedAt[] mixedBricks, int baseIndex) {
        for (int index = 0; index < mixedBricks.length; index++) {
            if (mixedBricks[index].baseIndex() == baseIndex) {
                return index;
            }
        }
        return -1;
    }

    private static int baseWorldX(int baseIndex) {
        return (baseIndex & 3) * 4;
    }

    private static int baseWorldY(int baseIndex) {
        return ((baseIndex >>> 4) & 3) * 4;
    }

    private static int baseWorldZ(int baseIndex) {
        return ((baseIndex >>> 2) & 3) * 4;
    }

    private static double sumLiveEnthalpy(ThermalCellArena arena) {
        double total = 0.0D;
        for (int slot = 0; slot < arena.highWaterMark(); slot++) {
            if (arena.isLive(slot)) {
                total += arena.enthalpyJ(slot);
            }
        }
        return total;
    }

    private record MixedAt(
            int baseIndex,
            ComponentBrickCompiler.CompiledBrick geometry
    ) {
    }

    private record PageFixture(
            ThermalPage page,
            ArenaSpan span,
            ImplicitAirAdjacency.PageView view,
            int[] mixedSupportRefs
    ) {
        private PageFixture {
            mixedSupportRefs = mixedSupportRefs.clone();
        }

        @Override
        public int[] mixedSupportRefs() {
            return mixedSupportRefs.clone();
        }
    }
}
