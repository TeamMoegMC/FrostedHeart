/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalSweepTest {
    private static final double EPSILON = 1.0e-8D;
    private static final BuoyancyConductance.Parameters NEUTRAL_BUOYANCY =
            new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D);

    @Test
    void closedMultiPairSweepKeepsTotalEnthalpyConstant() {
        double[] enthalpies = {100_000.0D, -30_000.0D, 5_000.0D, 80_000.0D};
        double[] capacities = {1_000.0D, 500.0D, 2_000.0D, 4_000.0D};
        ThermalCellArena arena = arena(enthalpies, capacities);
        ThermalSweep sweep = new ThermalSweep(
                arena,
                List.of(
                        ThermalSweep.PairOperation.fixed(0, 1, 75.0D),
                        ThermalSweep.PairOperation.fixed(1, 2, 50.0D),
                        ThermalSweep.PairOperation.fixed(2, 3, 25.0D),
                        ThermalSweep.PairOperation.fixed(0, 3, 10.0D)),
                List.of(),
                NEUTRAL_BUOYANCY);
        double initial = sum(arena, enthalpies.length);

        ThermalSweep.Result result = sweep.apply(
                0.0D, epoch(1L));

        assertEquals(4, result.appliedPairs());
        assertEquals(0, result.appliedBoundaries());
        assertEquals(0, result.numericDegradedOperations());
        assertEquals(initial, sum(arena, enthalpies.length), EPSILON);
        assertEquals(0.0D, result.conservationResidualJ(), EPSILON);
    }

    @Test
    void forwardAndReverseExposeSplittingErrorWhileBothRemainConservative() {
        double[] initial = {100_000.0D, 0.0D, 0.0D};
        double[] capacities = {1_000.0D, 1_000.0D, 1_000.0D};
        ThermalCellArena forwardArena = arena(initial, capacities);
        ThermalCellArena reverseArena = arena(initial, capacities);
        List<ThermalSweep.PairOperation> operations = List.of(
                ThermalSweep.PairOperation.fixed(0, 1, 300.0D),
                ThermalSweep.PairOperation.fixed(1, 2, 300.0D));
        ThermalSweep forwardSweep = new ThermalSweep(
                forwardArena, operations, List.of(), NEUTRAL_BUOYANCY);
        ThermalSweep reverseSweep = new ThermalSweep(
                reverseArena, operations, List.of(), NEUTRAL_BUOYANCY);

        ThermalSweep.Result forward = forwardSweep.apply(
                0.0D, epoch(1L),
                ThermalSweep.Direction.FORWARD);
        ThermalSweep.Result reverse = reverseSweep.apply(
                0.0D, epoch(2L),
                ThermalSweep.Direction.REVERSE);

        assertNotEquals(forwardArena.enthalpyJ(2), reverseArena.enthalpyJ(2));
        assertTrue(forwardArena.enthalpyJ(2) > reverseArena.enthalpyJ(2));
        assertEquals(100_000.0D, sum(forwardArena, initial.length), EPSILON);
        assertEquals(100_000.0D, sum(reverseArena, initial.length), EPSILON);
        assertEquals(0.0D, forward.conservationResidualJ(), EPSILON);
        assertEquals(0.0D, reverse.conservationResidualJ(), EPSILON);
        assertEquals(ThermalSweep.Direction.FORWARD,
                ThermalSweep.Direction.forEpoch(epoch(3L)));
        assertEquals(ThermalSweep.Direction.REVERSE,
                ThermalSweep.Direction.forEpoch(epoch(4L)));
    }

    @Test
    void boundaryEnergyExactlyExplainsOpenSystemSumChange() {
        double[] enthalpies = {0.0D, 20_000.0D};
        double[] capacities = {1_000.0D, 1_000.0D};
        ThermalCellArena arena = arena(enthalpies, capacities);
        ThermalSweep sweep = new ThermalSweep(
                arena,
                List.of(ThermalSweep.PairOperation.fixed(0, 1, 20.0D)),
                List.of(
                        new ThermalSweep.BoundaryOperation(0, 100.0D, 10.0D),
                        new ThermalSweep.BoundaryOperation(1, -20.0D, 5.0D)),
                NEUTRAL_BUOYANCY);
        double initial = sum(arena, enthalpies.length);

        ThermalSweep.Result result = sweep.apply(
                0.0D, epoch(1L));

        assertEquals(initial + result.boundaryEnergyJ(),
                sum(arena, enthalpies.length), EPSILON);
        assertEquals(0.0D, result.conservationResidualJ(), EPSILON);
        assertEquals(2, result.appliedBoundaries());
    }

    @Test
    void buoyancyUsesTheCurrentArenaTemperatures() {
        BuoyancyConductance.Parameters parameters =
                new BuoyancyConductance.Parameters(0.25D, 4.0D, 20.0D);
        double[] hotBelow = {40_000.0D, 0.0D};
        double[] coldBelow = {0.0D, 40_000.0D};
        double[] capacities = {1_000.0D, 1_000.0D};
        ThermalCellArena hotArena = arena(hotBelow, capacities);
        ThermalCellArena coldArena = arena(coldBelow, capacities);
        ThermalSweep hotSweep = buoyantSweep(hotArena, parameters);
        ThermalSweep coldSweep = buoyantSweep(coldArena, parameters);

        hotSweep.apply(0.0D, epoch(1L));
        coldSweep.apply(0.0D, epoch(1L));

        double unstableTransfer = 40_000.0D - hotArena.enthalpyJ(0);
        double stableTransfer = coldArena.enthalpyJ(0);
        assertTrue(unstableTransfer > stableTransfer);
        assertEquals(40_000.0D, sum(hotArena, 2), EPSILON);
        assertEquals(40_000.0D, sum(coldArena, 2), EPSILON);
    }

    @Test
    void malformedCompiledOperationsAreRejectedBeforeMutation() {
        ThermalCellArena arena = arena(
                new double[]{0.0D, 0.0D}, new double[]{1.0D, 1.0D});
        assertThrows(IllegalArgumentException.class, () -> new ThermalSweep(
                arena,
                List.of(ThermalSweep.PairOperation.fixed(0, 2, 10.0D)),
                List.of(),
                NEUTRAL_BUOYANCY));
        assertThrows(IllegalArgumentException.class,
                () -> ThermalSweep.PairOperation.fixed(0, 1, -1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new ThermalSweep.BoundaryOperation(0, Double.NaN, 1.0D));
    }

    @Test
    void reusedArenaSlotsInvalidateTheWholeSweepBeforeMutation() {
        ThermalCellArena arena = new ThermalCellArena(0);
        ArenaSpan parent = arena.allocatePageCells(
                0,
                1,
                new ThermalCellArena.CellSpec[]{new ThermalCellArena.CellSpec(
                        0, 0, 0, 8, 0, 0, 512.0D)},
                new double[]{0.0D});
        ThermalCellArena.CellSpec[] children = new ThermalCellArena.CellSpec[8];
        int write = 0;
        for (int y = 0; y < 8; y += 4) {
            for (int z = 0; z < 8; z += 4) {
                for (int x = 0; x < 8; x += 4) {
                    children[write++] = new ThermalCellArena.CellSpec(
                            x, y, z, 4, 0, 0, 64.0D);
                }
            }
        }
        ThermalCellArena.PageLayoutReplacement replacement =
                arena.prepareSplitPureLod(parent, parent.firstSlot(), children);
        int first = replacement.newSpan().firstSlot();
        ThermalSweep stale = new ThermalSweep(
                arena,
                List.of(ThermalSweep.PairOperation.fixed(first, first + 1, 10.0D)),
                List.of(),
                NEUTRAL_BUOYANCY);
        replacement.rollback();
        ArenaSpan reused = arena.allocatePageCells(
                1,
                2,
                new ThermalCellArena.CellSpec[]{
                        new ThermalCellArena.CellSpec(16, 0, 0, 4, 0, 0, 64.0D),
                        new ThermalCellArena.CellSpec(20, 0, 0, 4, 0, 0, 64.0D)},
                new double[]{11.0D, 22.0D});
        assertEquals(first, reused.firstSlot());

        assertThrows(IllegalStateException.class, () ->
                stale.apply(0.0D, epoch(1L)));
        assertEquals(11.0D, arena.enthalpyJ(first), EPSILON);
        assertEquals(22.0D, arena.enthalpyJ(first + 1), EPSILON);
    }

    private static ThermalSweep buoyantSweep(
            ThermalCellArena arena,
            BuoyancyConductance.Parameters parameters
    ) {
        return new ThermalSweep(
                arena,
                List.of(ThermalSweep.PairOperation.buoyant(0, 1, 10.0D, 0.0D, 4.0D)),
                List.of(),
                parameters);
    }

    private static SolveEpoch epoch(long id) {
        return new SolveEpoch(0L, 5L, id, 1L, InputWatermarks.ZERO);
    }

    private static ThermalCellArena arena(double[] enthalpies, double[] capacities) {
        ThermalCellArena.CellSpec[] cells = new ThermalCellArena.CellSpec[enthalpies.length];
        for (int index = 0; index < cells.length; index++) {
            int x = (index & 3) * 4;
            int z = ((index >>> 2) & 3) * 4;
            int y = ((index >>> 4) & 3) * 4;
            cells[index] = new ThermalCellArena.CellSpec(
                    x, y, z, 4, 0, 0, capacities[index]);
        }
        ThermalCellArena arena = new ThermalCellArena(0);
        arena.allocatePageCells(0, 1, cells, enthalpies);
        return arena;
    }

    private static double sum(ThermalCellArena arena, int count) {
        double result = 0.0D;
        for (int slot = 0; slot < count; slot++) {
            result += arena.enthalpyJ(slot);
        }
        return result;
    }
}
