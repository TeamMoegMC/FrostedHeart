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
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometrySummary;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometrySummaryCache;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalCellArenaTest {
    private static final double EPSILON = 1.0e-9D;
    private static final double AIR_CAPACITY_DENSITY = 2.5D;

    @Test
    void regularCellStoresPrimitiveHCAuthorityAndDerivesTemperature() {
        ThermalCellArena arena = new ThermalCellArena(0);
        ThermalCellArena.CellSpec cell = regularCell(0, 0, 0, 16, 3, 5);

        ArenaSpan span = arena.allocatePageCells(
                7,
                new ThermalCellArena.CellSpec[]{cell},
                -12.0D,
                0.0D
        );
        int slot = span.firstSlot();

        assertEquals(1, span.count());
        assertEquals(1, arena.liveCellCount());
        assertEquals(7, arena.pageSlot(slot));
        assertEquals(slot, arena.supportRef(slot));
        assertEquals(2, arena.level(slot));
        assertEquals(16, arena.widthBlocks(slot));
        assertEquals(3, arena.mediumId(slot));
        assertEquals(5, arena.flags(slot));
        assertEquals(AIR_CAPACITY_DENSITY * 4096.0D,
                arena.capacityJPerK(slot), EPSILON);
        assertEquals(-12.0D, arena.temperatureC(slot, 0.0D), EPSILON);

        arena.addEnthalpyJ(slot, arena.capacityJPerK(slot) * 2.0D);
        assertEquals(-10.0D, arena.temperatureC(slot, 0.0D), EPSILON);
        assertThrows(IllegalArgumentException.class, () ->
                arena.setEnthalpyJ(slot, Double.NaN));
    }

    @Test
    void mixedBrickComponentsShareTheArenaAuthorityAndCompiledGeometry() {
        ThermalCellArena arena = new ThermalCellArena(0);
        ComponentBrickCompiler.CompiledBrick geometry = splitAirBrick();

        ThermalCellArena.PageAllocation allocation = arena.allocatePageCells(
                8,
                7,
                new ThermalCellArena.CellSpec[0],
                new ThermalCellArena.MixedBrickSpec[]{new ThermalCellArena.MixedBrickSpec(
                        32, 0, -16, geometry, 4, 3, AIR_CAPACITY_DENSITY)},
                10.0D,
                0.0D
        );
        int support = allocation.mixedSupportRefs()[0];
        int left = arena.mixedComponentSlot(support, 0);
        int right = arena.mixedComponentSlot(support, 1);

        assertEquals(2, allocation.cellSpan().count());
        assertEquals(support, left);
        assertEquals(support, arena.supportRef(right));
        assertTrue(arena.isMixedSupport(support));
        assertTrue(arena.isMixedComponent(right));
        assertEquals(7, arena.lifecycleGeneration(left));
        assertEquals(16.0D * AIR_CAPACITY_DENSITY,
                arena.capacityJPerK(left), EPSILON);
        assertEquals(32.0D * AIR_CAPACITY_DENSITY,
                arena.capacityJPerK(right), EPSILON);
        assertEquals(32.5D, arena.centerX(left), EPSILON);
        assertEquals(35.0D, arena.centerX(right), EPSILON);
        assertEquals(2.0D, arena.centerY(left), EPSILON);
        assertEquals(-14.0D, arena.centerZ(right), EPSILON);
        assertEquals(10.0D, arena.temperatureC(left, 0.0D), EPSILON);
    }

    @Test
    void mixedLayoutRejectsMisalignmentAndOverlappingSupports() {
        ComponentBrickCompiler.CompiledBrick geometry = fullAirBrick();
        assertThrows(IllegalArgumentException.class, () ->
                new ThermalCellArena.MixedBrickSpec(
                        1, 0, 0, geometry, 0, 0, AIR_CAPACITY_DENSITY));

        ThermalCellArena arena = new ThermalCellArena(0);
        ThermalCellArena.MixedBrickSpec mixed = new ThermalCellArena.MixedBrickSpec(
                0, 0, 0, geometry, 0, 0, AIR_CAPACITY_DENSITY);
        assertThrows(IllegalArgumentException.class, () -> arena.allocatePageCells(
                0,
                1,
                new ThermalCellArena.CellSpec[]{regularCell(0, 0, 0, 4, 0, 0)},
                new ThermalCellArena.MixedBrickSpec[]{mixed},
                0.0D,
                0.0D));
        assertThrows(IllegalArgumentException.class, () -> arena.allocatePageCells(
                0,
                1,
                new ThermalCellArena.CellSpec[0],
                new ThermalCellArena.MixedBrickSpec[]{mixed, mixed},
                0.0D,
                0.0D));
    }

    @Test
    void lifecycleCheckedNodeWriteRejectsStaleGenerationBeforeMutation() {
        ThermalCellArena arena = new ThermalCellArena(0);
        ArenaSpan span = arena.allocatePageCells(
                0,
                2,
                new ThermalCellArena.CellSpec[]{regularCell(0, 0, 0, 4, 0, 0)},
                new double[]{10.0D});

        assertThrows(IllegalStateException.class, () ->
                arena.addNodeEnthalpyJ(span.firstSlot(), 1, 5.0D));
        assertEquals(10.0D, arena.enthalpyJ(span.firstSlot()), EPSILON);
        arena.addNodeEnthalpyJ(span.firstSlot(), 2, 5.0D);
        assertEquals(15.0D, arena.enthalpyJ(span.firstSlot()), EPSILON);
    }

    @Test
    void densePageSplitAndMergeConserveEnthalpyAcrossCoverageHandoff() {
        ThermalCellArena arena = new ThermalCellArena(1);
        ThermalCellArena.CellSpec parent = regularCell(0, 0, 0, 16, 0, 0);
        ArenaSpan parentSpan = arena.allocatePageCells(
                4,
                new ThermalCellArena.CellSpec[]{parent},
                20.0D,
                0.0D
        );
        ThermalPage page = ThermalPage.allAir(
                100L, 1L, parentSpan.firstSlot(), 0);
        page.setCellSpan(parentSpan);
        double originalEnthalpy = arena.enthalpyJ(parentSpan.firstSlot());

        ThermalCellArena.CellSpec[] fineCells = uniformCells(0, 0, 0, 16, 4, 0, 0);
        ThermalCellArena.PageLayoutReplacement split = arena.prepareSplitPureLod(
                parentSpan,
                parentSpan.firstSlot(),
                fineCells
        );

        assertEquals(ThermalCellArena.ReplacementKind.SPLIT, split.kind());
        assertEquals(65, arena.liveCellCount());
        assertTrue(arena.isLive(parentSpan.firstSlot()));
        assertEquals(originalEnthalpy, split.newEnthalpyJ(), EPSILON);
        assertEquals(0.0D, split.energyResidualJ(), EPSILON);
        for (int slot = split.newSpan().firstSlot();
                slot < split.newSpan().endSlotExclusive();
                slot++) {
            assertEquals(20.0D, arena.temperatureC(slot, 0.0D), EPSILON);
        }

        assertTrue(page.tryInstallGeometryBuild(
                page.liveGeometryRevision(),
                regularAirState(arena, split.newSpan(), 0, 0, 0, 0)
        ));
        split.commit(page);
        assertEquals(ThermalCellArena.ReplacementState.COMMITTED, split.state());
        assertFalse(arena.isLive(parentSpan.firstSlot()));
        assertEquals(64, arena.liveCellCount());

        int firstChild = split.newSpan().firstSlot();
        arena.addEnthalpyJ(firstChild, 123.0D);
        double expectedMergedEnthalpy = sumEnthalpy(arena, split.newSpan());
        int[] childSlots = slots(split.newSpan());
        ThermalCellArena.PageLayoutReplacement merge = arena.prepareMergePureLod(
                split.newSpan(),
                childSlots,
                parent
        );

        assertEquals(ThermalCellArena.ReplacementKind.MERGE, merge.kind());
        assertEquals(expectedMergedEnthalpy, merge.newEnthalpyJ(), EPSILON);
        assertTrue(page.tryInstallGeometryBuild(
                page.liveGeometryRevision(),
                regularAirState(arena, merge.newSpan(), 0, 0, 0, 0)
        ));
        merge.commit(page);

        assertEquals(1, arena.liveCellCount());
        assertEquals(expectedMergedEnthalpy,
                arena.enthalpyJ(merge.newSpan().firstSlot()), EPSILON);
        assertEquals(
                expectedMergedEnthalpy / parent.capacityJPerK(),
                arena.temperatureC(merge.newSpan().firstSlot(), 0.0D),
                EPSILON
        );
    }

    @Test
    void failedPageInstallCanRollbackPreparedSplitWithoutLosingOldState() {
        ThermalCellArena arena = new ThermalCellArena(0);
        ThermalCellArena.CellSpec parent = regularCell(-16, 0, 0, 8, 1, 0);
        ArenaSpan oldSpan = arena.allocatePageCells(
                2,
                new ThermalCellArena.CellSpec[]{parent},
                new double[]{-77.0D}
        );

        ThermalCellArena.PageLayoutReplacement replacement =
                arena.prepareSplitPureLod(
                        oldSpan,
                        oldSpan.firstSlot(),
                        uniformCells(-16, 0, 0, 8, 4, 1, 0)
                );
        ArenaSpan preparedSpan = replacement.newSpan();

        assertEquals(9, arena.liveCellCount());
        replacement.rollback();

        assertEquals(ThermalCellArena.ReplacementState.ROLLED_BACK, replacement.state());
        assertEquals(1, arena.liveCellCount());
        assertTrue(arena.isLive(oldSpan.firstSlot()));
        assertEquals(-77.0D, arena.enthalpyJ(oldSpan.firstSlot()), EPSILON);
        assertFalse(arena.isLive(preparedSpan.firstSlot()));
        assertThrows(IllegalStateException.class, () ->
                replacement.newCellSlotCovering(-16, 0, 0));
    }

    @Test
    void splitMergeRejectInvalidPartitionCapacityAndOwnership() {
        ThermalCellArena arena = new ThermalCellArena(0);
        ThermalCellArena.CellSpec parent = regularCell(0, 0, 0, 8, 0, 0);
        ArenaSpan parentSpan = arena.allocatePageCells(
                0,
                new ThermalCellArena.CellSpec[]{parent},
                0.0D,
                0.0D
        );
        ThermalCellArena.CellSpec[] incomplete = Arrays.copyOf(
                uniformCells(0, 0, 0, 8, 4, 0, 0), 7);

        assertThrows(IllegalArgumentException.class, () ->
                arena.prepareSplitPureLod(parentSpan, parentSpan.firstSlot(), incomplete));

        ThermalCellArena.CellSpec[] wrongDensity = uniformCells(
                0, 0, 0, 8, 4, 0, 0);
        wrongDensity[0] = ThermalCellArena.CellSpec.regularAir(
                0, 0, 0, 4, 0, 0, AIR_CAPACITY_DENSITY * 2.0D);
        assertThrows(IllegalArgumentException.class, () ->
                arena.prepareSplitPureLod(parentSpan, parentSpan.firstSlot(), wrongDensity));

        ArenaSpan foreign = arena.allocatePageCells(
                1,
                new ThermalCellArena.CellSpec[]{regularCell(16, 0, 0, 4, 0, 0)},
                0.0D,
                0.0D
        );
        assertThrows(IllegalArgumentException.class, () ->
                arena.prepareMergePureLod(
                        parentSpan,
                        new int[]{parentSpan.firstSlot(), foreign.firstSlot()},
                        parent
                ));
        assertThrows(IllegalArgumentException.class, () ->
                arena.allocatePageCells(
                        2,
                        new ThermalCellArena.CellSpec[]{
                                regularCell(32, 0, 0, 8, 0, 0),
                                regularCell(36, 0, 0, 4, 0, 0)
                        },
                        0.0D,
                        0.0D
                ));
    }

    @Test
    void replacementCannotReleaseOldSpanBeforePageInstallsNewCoverage() {
        ThermalCellArena arena = new ThermalCellArena(0);
        ThermalCellArena.CellSpec parent = regularCell(0, 0, 0, 8, 0, 0);
        ArenaSpan oldSpan = arena.allocatePageCells(
                3,
                new ThermalCellArena.CellSpec[]{parent},
                0.0D,
                0.0D
        );
        ThermalPage page = ThermalPage.allAir(101L, 1L, oldSpan.firstSlot(), 0);
        page.setCellSpan(oldSpan);
        ThermalCellArena.PageLayoutReplacement replacement =
                arena.prepareSplitPureLod(
                        oldSpan,
                        oldSpan.firstSlot(),
                        uniformCells(0, 0, 0, 8, 4, 0, 0)
                );

        assertThrows(IllegalStateException.class, () -> replacement.commit(page));
        assertTrue(arena.isLive(oldSpan.firstSlot()));
        assertEquals(ThermalCellArena.ReplacementState.PREPARED, replacement.state());
        replacement.rollback();
    }

    private static ThermalCellArena.CellSpec regularCell(
            int minX,
            int minY,
            int minZ,
            int width,
            int medium,
            int flags
    ) {
        return ThermalCellArena.CellSpec.regularAir(
                minX,
                minY,
                minZ,
                width,
                medium,
                flags,
                AIR_CAPACITY_DENSITY
        );
    }

    private static ThermalCellArena.CellSpec[] uniformCells(
            int minX,
            int minY,
            int minZ,
            int parentWidth,
            int childWidth,
            int medium,
            int flags
    ) {
        List<ThermalCellArena.CellSpec> cells = new ArrayList<>();
        for (int y = 0; y < parentWidth; y += childWidth) {
            for (int z = 0; z < parentWidth; z += childWidth) {
                for (int x = 0; x < parentWidth; x += childWidth) {
                    cells.add(regularCell(
                            minX + x, minY + y, minZ + z,
                            childWidth, medium, flags));
                }
            }
        }
        return cells.toArray(ThermalCellArena.CellSpec[]::new);
    }

    private static ThermalPage.FullGeometryState regularAirState(
            ThermalCellArena arena,
            ArenaSpan span,
            int pageMinX,
            int pageMinY,
            int pageMinZ,
            int mediumId
    ) {
        int[] refs = new int[ThermalPage.BASE_BRICK_COUNT];
        byte[] widths = new byte[ThermalPage.BASE_BRICK_COUNT];
        for (int baseIndex = 0; baseIndex < ThermalPage.BASE_BRICK_COUNT; baseIndex++) {
            int brickX = baseIndex & 3;
            int brickZ = (baseIndex >>> 2) & 3;
            int brickY = (baseIndex >>> 4) & 3;
            int worldX = pageMinX + brickX * 4;
            int worldY = pageMinY + brickY * 4;
            int worldZ = pageMinZ + brickZ * 4;
            int slot = cellCovering(arena, span, worldX, worldY, worldZ);
            refs[baseIndex] = slot;
            widths[baseIndex] = (byte) arena.widthBlocks(slot);
        }
        GeometrySummary[] summaries = new GeometrySummary[GeometrySummaryCache.SUMMARY_COUNT];
        Arrays.fill(summaries, GeometrySummary.singleAir(mediumId));
        return new ThermalPage.FullGeometryState(
                refs,
                widths,
                summaries,
                0L,
                span
        );
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
            if (worldX >= arena.minimumX(slot) && worldX < arena.minimumX(slot) + width
                    && worldY >= arena.minimumY(slot) && worldY < arena.minimumY(slot) + width
                    && worldZ >= arena.minimumZ(slot) && worldZ < arena.minimumZ(slot) + width) {
                return slot;
            }
        }
        throw new AssertionError("missing regular cell coverage");
    }

    private static int[] slots(ArenaSpan span) {
        int[] result = new int[span.count()];
        for (int index = 0; index < result.length; index++) {
            result[index] = span.firstSlot() + index;
        }
        return result;
    }

    private static double sumEnthalpy(ThermalCellArena arena, ArenaSpan span) {
        double total = 0.0D;
        for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++) {
            total += arena.enthalpyJ(slot);
        }
        return total;
    }

    private static ComponentBrickCompiler.CompiledBrick splitAirBrick() {
        List<ConservativeAirGeometry.Resolution> blocks = repeated(air());
        for (int y = 0; y < 4; y++) {
            for (int z = 0; z < 4; z++) {
                blocks.set(ComponentBrickCompiler.blockIndex(1, y, z), solid());
            }
        }
        return resolved(ComponentBrickCompiler.compile(blocks, 4, 0));
    }

    private static ComponentBrickCompiler.CompiledBrick fullAirBrick() {
        return resolved(ComponentBrickCompiler.compile(repeated(air()), 4, 0));
    }

    private static ComponentBrickCompiler.CompiledBrick resolved(
            ComponentBrickCompiler.Compilation compilation
    ) {
        assertEquals(ComponentBrickCompiler.Status.RESOLVED, compilation.status());
        return compilation.brick().orElseThrow();
    }

    private static ConservativeAirGeometry.Resolution air() {
        return ConservativeAirGeometry.resolve(List.of(), 4);
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
}
