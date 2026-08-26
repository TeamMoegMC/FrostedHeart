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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalCellArenaTest {
    private static final double EPSILON = 1.0e-9D;
    private static final double AIR_CAPACITY_DENSITY = 2.5D;

    @Test
    void regularBrickStoresPrimitiveHCAuthorityAndDerivesTemperature() {
        ThermalCellArena arena = new ThermalCellArena(0);
        ArenaSpan span = arena.allocatePageCells(
                7,
                0,
                new ThermalCellArena.CellSpec[]{regularCell(0, 0, 0, 3, 5)},
                new ThermalCellArena.MixedBrickSpec[0],
                new ThermalCellArena.MaterialPoleSpec[0],
                new ThermalCellArena.PhaseReservoirSpec[0],
                -12.0D,
                0.0D).cellSpan();
        int slot = span.firstSlot();

        assertEquals(1, span.count());
        assertEquals(1, arena.liveCellCount());
        assertEquals(7, arena.pageSlot(slot));
        assertEquals(slot, arena.supportRef(slot));
        assertEquals(3, arena.mediumId(slot));
        assertEquals(5, arena.flags(slot));
        assertEquals(AIR_CAPACITY_DENSITY * 64.0D,
                arena.capacityJPerK(slot), EPSILON);
        assertEquals(-12.0D, arena.temperatureC(slot, 0.0D), EPSILON);

        arena.addEnthalpyJ(slot, arena.capacityJPerK(slot) * 2.0D);
        assertEquals(-10.0D, arena.temperatureC(slot, 0.0D), EPSILON);
        assertThrows(IllegalArgumentException.class, () ->
                arena.setEnthalpyJ(slot, Double.NaN));
    }

    @Test
    void mixedBrickComponentsShareArenaAuthorityAndCompiledGeometry() {
        ThermalCellArena arena = new ThermalCellArena(0);
        ComponentBrickCompiler.CompiledBrick geometry = splitAirBrick();
        ThermalCellArena.PageAllocation allocation = arena.allocatePageCells(
                8,
                7,
                new ThermalCellArena.CellSpec[0],
                new ThermalCellArena.MixedBrickSpec[]{new ThermalCellArena.MixedBrickSpec(
                        32, 0, -16, geometry, 4, 3, AIR_CAPACITY_DENSITY)},
                new ThermalCellArena.MaterialPoleSpec[0],
                new ThermalCellArena.PhaseReservoirSpec[0],
                10.0D,
                0.0D);
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
                0, 1,
                new ThermalCellArena.CellSpec[]{regularCell(0, 0, 0, 0, 0)},
                new ThermalCellArena.MixedBrickSpec[]{mixed},
                new ThermalCellArena.MaterialPoleSpec[0],
                new ThermalCellArena.PhaseReservoirSpec[0],
                0.0D, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> arena.allocatePageCells(
                0, 1,
                new ThermalCellArena.CellSpec[0],
                new ThermalCellArena.MixedBrickSpec[]{mixed, mixed},
                new ThermalCellArena.MaterialPoleSpec[0],
                new ThermalCellArena.PhaseReservoirSpec[0],
                0.0D, 0.0D));
    }

    @Test
    void lifecycleCheckedNodeWriteRejectsStaleGenerationBeforeMutation() {
        ThermalCellArena arena = new ThermalCellArena(0);
        ArenaSpan span = arena.allocatePageCells(
                0,
                2,
                new ThermalCellArena.CellSpec[]{regularCell(0, 0, 0, 0, 0)},
                new ThermalCellArena.MixedBrickSpec[0],
                new ThermalCellArena.MaterialPoleSpec[0],
                new ThermalCellArena.PhaseReservoirSpec[0],
                0.0D,
                0.0D).cellSpan();
        arena.setEnthalpyJ(span.firstSlot(), 10.0D);

        assertThrows(IllegalStateException.class, () ->
                arena.addNodeEnthalpyJ(span.firstSlot(), 1, 5.0D));
        assertEquals(10.0D, arena.enthalpyJ(span.firstSlot()), EPSILON);
        arena.addNodeEnthalpyJ(span.firstSlot(), 2, 5.0D);
        assertEquals(15.0D, arena.enthalpyJ(span.firstSlot()), EPSILON);
    }

    @Test
    void fragmentedArenaUsesBestFitSpansWithoutGrowingHighWater() {
        ThermalCellArena arena = new ThermalCellArena(0);
        ArenaSpan small = allocateRegularSpan(arena, 0, 1, 3);
        allocateRegularSpan(arena, 1, 2, 1);
        ArenaSpan large = allocateRegularSpan(arena, 2, 3, 5);
        allocateRegularSpan(arena, 3, 4, 1);
        assertEquals(10, arena.highWaterMark());

        arena.releasePageCells(0, 1, small);
        arena.releasePageCells(2, 3, large);

        ArenaSpan four = allocateRegularSpan(arena, 4, 5, 4);
        ArenaSpan three = allocateRegularSpan(arena, 5, 6, 3);
        assertEquals(4, four.firstSlot());
        assertEquals(0, three.firstSlot());
        assertEquals(10, arena.highWaterMark());
    }

    @Test
    void adjacentFreeSpansCoalesceAndTrimTheArenaTail() {
        ThermalCellArena arena = new ThermalCellArena(0);
        ArenaSpan first = allocateRegularSpan(arena, 0, 1, 2);
        ArenaSpan middle = allocateRegularSpan(arena, 1, 2, 2);
        ArenaSpan tail = allocateRegularSpan(arena, 2, 3, 2);
        assertEquals(6, arena.highWaterMark());

        arena.releasePageCells(1, 2, middle);
        arena.releasePageCells(0, 1, first);
        ArenaSpan replacement = allocateRegularSpan(arena, 3, 4, 3);
        assertEquals(0, replacement.firstSlot());
        assertEquals(6, arena.highWaterMark());

        arena.releasePageCells(2, 3, tail);
        assertEquals(3, arena.highWaterMark());
        arena.releasePageCells(3, 4, replacement);
        assertEquals(0, arena.highWaterMark());
        assertEquals(0, arena.liveCellCount());
    }

    private static ArenaSpan allocateRegularSpan(
            ThermalCellArena arena,
            int pageSlot,
            int lifecycleGeneration,
            int count
    ) {
        ThermalCellArena.CellSpec[] cells = new ThermalCellArena.CellSpec[count];
        for (int index = 0; index < count; index++) {
            cells[index] = regularCell(
                    (index & 3) << 2,
                    ((index >>> 4) & 3) << 2,
                    ((index >>> 2) & 3) << 2,
                    0,
                    0);
        }
        return arena.allocatePageCells(
                pageSlot,
                lifecycleGeneration,
                cells,
                new ThermalCellArena.MixedBrickSpec[0],
                new ThermalCellArena.MaterialPoleSpec[0],
                new ThermalCellArena.PhaseReservoirSpec[0],
                0.0D,
                0.0D).cellSpan();
    }

    private static ThermalCellArena.CellSpec regularCell(
            int x, int y, int z, int medium, int flags
    ) {
        return ThermalCellArena.CellSpec.regularAir(
                x, y, z, medium, flags, AIR_CAPACITY_DENSITY);
    }

    private static ComponentBrickCompiler.CompiledBrick splitAirBrick() {
        List<ConservativeAirGeometry.Resolution> blocks = repeated(solid());
        for (int y = 0; y < 4; y++) {
            for (int z = 0; z < 4; z++) {
                blocks.set(ComponentBrickCompiler.blockIndex(0, y, z), air());
            }
        }
        for (int y = 0; y < 4; y++) {
            for (int z = 0; z < 4; z++) {
                for (int x = 2; x < 4; x++) {
                    blocks.set(ComponentBrickCompiler.blockIndex(x, y, z), air());
                }
            }
        }
        return resolved(ComponentBrickCompiler.compile(blocks, 4));
    }

    private static ComponentBrickCompiler.CompiledBrick fullAirBrick() {
        return resolved(ComponentBrickCompiler.compile(repeated(air()), 4));
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
