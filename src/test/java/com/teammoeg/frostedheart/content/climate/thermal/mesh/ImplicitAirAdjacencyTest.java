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
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweepFragments;
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
    private static final ImplicitAirAdjacency.PositiveNeighbors NO_POSITIVE_NEIGHBORS =
            new ImplicitAirAdjacency.PositiveNeighbors(null, null, null);

    @Test
    void finePageCompilesAllInternalFacesWithoutPersistingEdges() {
        ThermalCellArena arena = new ThermalCellArena(0);
        PageFixture page = regularPage(
                arena, 0, 0, 0, 0,
                uniformCells(0, 0, 0, 16), true);

        ImplicitAirAdjacency.CompiledPairs result = compile(
                page.view(), NO_POSITIVE_NEIGHBORS, arena);

        assertTrue(result.ownerPublicationCurrent());
        assertEquals(144, result.operations().size());
        assertEquals(144.0D * 16.0D,
                result.totalOpenAreaBlocksSquared(), EPSILON);
        assertEquals(0, result.mixedPairCount());
        assertEquals(0, result.unavailablePositivePages());
    }

    @Test
    void negativePageOwnsCrossPagePairsExactlyOnce() {
        ThermalCellArena arena = new ThermalCellArena(0);
        PageFixture negative = regularPage(
                arena, 0, 0, 0, 0,
                uniformCells(0, 0, 0, 16), true);
        PageFixture positive = regularPage(
                arena, 1, 16, 0, 0,
                uniformCells(16, 0, 0, 16), true);

        ImplicitAirAdjacency.CompiledPairs result = compile(
                negative.view(),
                new ImplicitAirAdjacency.PositiveNeighbors(
                        positive.view(), null, null),
                arena);

        assertEquals(160, result.operations().size());
        assertEquals(2_560.0D, result.totalOpenAreaBlocksSquared(), EPSILON);
        for (ThermalSweep.PairOperation operation : result.operations()) {
            assertEquals(4.0D, operation.baseConductanceWPerK(), EPSILON);
        }
    }

    @Test
    void staleOwnerStopsCompilationAndStalePositivePageRemainsFrontier() {
        ThermalCellArena arena = new ThermalCellArena(0);
        PageFixture owner = regularPage(
                arena, 0, 0, 0, 0,
                uniformCells(0, 0, 0, 16), true);
        PageFixture neighbor = regularPage(
                arena, 1, 16, 0, 0,
                uniformCells(16, 0, 0, 16), true);
        neighbor.page().recordGeometryMutation(
                0, 0, 0, 1L, new GeometryDeltaRing(1));

        ImplicitAirAdjacency.CompiledPairs frontier = compile(
                owner.view(),
                new ImplicitAirAdjacency.PositiveNeighbors(
                        neighbor.view(), null, null),
                arena);
        assertTrue(frontier.ownerPublicationCurrent());
        assertEquals(144, frontier.operations().size());
        assertEquals(2_304.0D, frontier.totalOpenAreaBlocksSquared(), EPSILON);
        assertEquals(1, frontier.unavailablePositivePages());

        owner.page().recordGeometryMutation(
                0, 0, 0, 2L, new GeometryDeltaRing(1));
        ImplicitAirAdjacency.CompiledPairs staleOwner = compile(
                owner.view(), NO_POSITIVE_NEIGHBORS, arena);
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
                page.view(), NO_POSITIVE_NEIGHBORS, arena);
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
                page.view(), NO_POSITIVE_NEIGHBORS, arena);
        ThermalSweep.PairOperation mixedPair = result.operations().stream()
                .filter(operation -> operation.cellA() == left && operation.cellB() == right)
                .findFirst()
                .orElseThrow();
        assertEquals(1, result.mixedPairCount());
        assertEquals(0.25D, mixedPair.baseConductanceWPerK(), EPSILON);

        double before = sumLiveEnthalpy(arena);
        ThermalSweepFragments.Builder sweepBuilder = ThermalSweepFragments.builder(
                arena, null, NEUTRAL_BUOYANCY, 1);
        sweepBuilder.setAirPairs(0, result.operations());
        ThermalSweep sweep = sweepBuilder.build();
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
                page.view(), NO_POSITIVE_NEIGHBORS, arena);

        assertEquals(0, result.mixedPairCount());
    }

    @Test
    void pageViewRejectsMisalignmentAndNeighborMustBePositiveAdjacent() {
        ThermalCellArena arena = new ThermalCellArena(0);
        PageFixture owner = regularPage(
                arena, 0, 0, 0, 0,
                uniformCells(0, 0, 0, 16), true);
        PageFixture wrongNeighbor = regularPage(
                arena, 1, 32, 0, 0,
                uniformCells(32, 0, 0, 16), true);

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
        ArenaSpan foreign = arena.allocatePageCells(
                1,
                1,
                uniformCells(16, 0, 0, 16),
                new ThermalCellArena.MixedBrickSpec[0],
                new ThermalCellArena.MaterialPoleSpec[0],
                new ThermalCellArena.PhaseReservoirSpec[0],
                0.0D,
                0.0D).cellSpan();
        ThermalPage corrupt = new ThermalPage(300L, 1L);
        assertTrue(corrupt.tryInstallGeometryBuild(
                0L,
                regularAirState(arena, foreign, 16, 0, 0)));
        assertTrue(corrupt.tryPublishGeometry(0L, corrupt.topologyGeneration(), 0L));

        assertThrows(IllegalStateException.class, () -> compile(
                new ImplicitAirAdjacency.PageView(corrupt, 0, 0, 0, 0),
                NO_POSITIVE_NEIGHBORS,
                arena));
    }

    private static ImplicitAirAdjacency.CompiledPairs compile(
            ImplicitAirAdjacency.PageView owner,
            ImplicitAirAdjacency.PositiveNeighbors neighbors,
            ThermalCellArena arena
    ) {
        List<ThermalSweep.PairOperation> operations = new ArrayList<>();
        double totalOpenArea = 0.0D;
        int mixedPairCount = 0;
        int unavailablePositivePages = 0;
        boolean ownerPublicationCurrent = true;
        for (int baseBrickIndex = 0;
             baseBrickIndex < ThermalPage.BASE_BRICK_COUNT;
             baseBrickIndex++) {
            ImplicitAirAdjacency.CompiledPairs brick =
                    ImplicitAirAdjacency.compileOwnedBrickPairs(
                            owner,
                            neighbors,
                            arena,
                            baseBrickIndex,
                            MIXING_W_PER_BLOCK_K,
                            MINIMUM_MIXED_DISTANCE,
                            false);
            operations.addAll(brick.operations());
            totalOpenArea += brick.totalOpenAreaBlocksSquared();
            mixedPairCount += brick.mixedPairCount();
            unavailablePositivePages = Math.max(
                    unavailablePositivePages,
                    brick.unavailablePositivePages());
            ownerPublicationCurrent &= brick.ownerPublicationCurrent();
        }
        return new ImplicitAirAdjacency.CompiledPairs(
                operations,
                totalOpenArea,
                mixedPairCount,
                unavailablePositivePages,
                ownerPublicationCurrent);
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
                pageSlot,
                1,
                cells,
                new ThermalCellArena.MixedBrickSpec[0],
                new ThermalCellArena.MaterialPoleSpec[0],
                new ThermalCellArena.PhaseReservoirSpec[0],
                0.0D,
                0.0D).cellSpan();
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
                        baseWorldZ(baseIndex)));
            }
        }

        ThermalCellArena.PageAllocation allocation = arena.allocatePageCells(
                0,
                1,
                regular.toArray(ThermalCellArena.CellSpec[]::new),
                specs,
                new ThermalCellArena.MaterialPoleSpec[0],
                new ThermalCellArena.PhaseReservoirSpec[0],
                0.0D,
                0.0D);
        int[] mixedSupports = allocation.mixedSupportRefs();
        int[] refs = new int[ThermalPage.BASE_BRICK_COUNT];
        GeometrySummary[] summaries =
                new GeometrySummary[GeometrySummaryCache.BASE_SUMMARY_COUNT];
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
            if (mixedIndex >= 0) {
                mixedMask |= 1L << baseIndex;
                summaries[baseIndex] = GeometrySummary.mixed(
                        GeometrySummary.MATERIAL_INTERFACE);
            }
        }
        ThermalPage page = new ThermalPage(100L, 1L);
        assertTrue(page.tryInstallGeometryBuild(0L, new ThermalPage.FullGeometryState(
                refs,
                summaries,
                mixedMask)));
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
            int parentWidth
    ) {
        List<ThermalCellArena.CellSpec> cells = new ArrayList<>();
        for (int y = 0; y < parentWidth; y += 4) {
            for (int z = 0; z < parentWidth; z += 4) {
                for (int x = 0; x < parentWidth; x += 4) {
                    cells.add(cell(minX + x, minY + y, minZ + z));
                }
            }
        }
        return cells.toArray(ThermalCellArena.CellSpec[]::new);
    }

    private static ThermalCellArena.CellSpec cell(
            int minX,
            int minY,
            int minZ
    ) {
        return ThermalCellArena.CellSpec.regularAir(
                minX,
                minY,
                minZ,
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
        for (int baseIndex = 0; baseIndex < ThermalPage.BASE_BRICK_COUNT; baseIndex++) {
            int worldX = pageMinX + baseWorldX(baseIndex);
            int worldY = pageMinY + baseWorldY(baseIndex);
            int worldZ = pageMinZ + baseWorldZ(baseIndex);
            int slot = cellCovering(arena, span, worldX, worldY, worldZ);
            refs[baseIndex] = slot;
        }
        GeometrySummary[] summaries =
                new GeometrySummary[GeometrySummaryCache.BASE_SUMMARY_COUNT];
        Arrays.fill(summaries, GeometrySummary.singleAir(0));
        return new ThermalPage.FullGeometryState(refs, summaries, 0L);
    }

    private static int cellCovering(
            ThermalCellArena arena,
            ArenaSpan span,
            int worldX,
            int worldY,
            int worldZ
    ) {
        for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++) {
            if (arena.isLive(slot)
                    && !arena.isMixedComponent(slot)
                    && !arena.isMaterialPole(slot)
                    && !arena.isPhaseReservoir(slot)
                    && worldX >= arena.minimumX(slot)
                    && worldX < arena.minimumX(slot) + 4
                    && worldY >= arena.minimumY(slot)
                    && worldY < arena.minimumY(slot) + 4
                    && worldZ >= arena.minimumZ(slot)
                    && worldZ < arena.minimumZ(slot) + 4) {
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
                ComponentBrickCompiler.compile(blocks, 4);
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
