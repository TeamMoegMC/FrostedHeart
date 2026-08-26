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
        ThermalSweep sweep = airSweep(
                arena,
                List.of(
                        ThermalSweep.PairOperation.fixed(0, 1, 75.0D),
                        ThermalSweep.PairOperation.fixed(1, 2, 50.0D),
                        ThermalSweep.PairOperation.fixed(2, 3, 25.0D),
                        ThermalSweep.PairOperation.fixed(0, 3, 10.0D)),
                List.of(),
                NEUTRAL_BUOYANCY);
        double initial = sum(arena, enthalpies.length);

        ThermalSweep.Result result = sweep.apply(0.0D, epoch(1L));

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
        ThermalSweep forwardSweep = airSweep(
                forwardArena, operations, List.of(), NEUTRAL_BUOYANCY);
        ThermalSweep reverseSweep = airSweep(
                reverseArena, operations, List.of(), NEUTRAL_BUOYANCY);

        ThermalSweep.Result forward = forwardSweep.apply(
                0.0D, epoch(1L), ThermalSweep.Direction.FORWARD);
        ThermalSweep.Result reverse = reverseSweep.apply(
                0.0D, epoch(2L), ThermalSweep.Direction.REVERSE);

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
        ThermalSweep sweep = airSweep(
                arena,
                List.of(ThermalSweep.PairOperation.fixed(0, 1, 20.0D)),
                List.of(
                        new ThermalSweep.BoundaryOperation(0, 100.0D, 10.0D),
                        new ThermalSweep.BoundaryOperation(1, -20.0D, 5.0D)),
                NEUTRAL_BUOYANCY);
        double initial = sum(arena, enthalpies.length);

        ThermalSweep.Result result = sweep.apply(0.0D, epoch(1L));

        assertEquals(initial + result.boundaryEnergyJ(),
                sum(arena, enthalpies.length), EPSILON);
        assertEquals(0.0D, result.conservationResidualJ(), EPSILON);
        assertEquals(2, result.appliedBoundaries());
    }

    @Test
    void materialAggregationIsIndependentOfPatchHistory() {
        ThermalCellArena firstArena = arena(
                new double[]{90_000.0D, -20_000.0D},
                new double[]{1_000.0D, 1_000.0D});
        ThermalCellArena secondArena = arena(
                new double[]{90_000.0D, -20_000.0D},
                new double[]{1_000.0D, 1_000.0D});
        ThermalSweep first = materialSweep(firstArena, 1.0D, 2.0D, 3.0D);
        ThermalSweep second = materialSweep(secondArena, 1.0D, 2.0D, 3.0D);

        first = replaceMaterial(first, 0, 4.0D);
        first = replaceMaterial(first, 2, 6.0D);
        second = replaceMaterial(second, 2, 6.0D);
        second = replaceMaterial(second, 0, 4.0D);

        ThermalSweep.Result firstResult = first.apply(0.0D, epoch(1L));
        ThermalSweep.Result secondResult = second.apply(0.0D, epoch(1L));

        assertEquals(firstArena.enthalpyJ(0), secondArena.enthalpyJ(0), EPSILON);
        assertEquals(firstArena.enthalpyJ(1), secondArena.enthalpyJ(1), EPSILON);
        assertEquals(firstResult, secondResult);
        assertEquals(1, first.pairOperationCount());
    }

    @Test
    void repeatedMaterialFragmentReplacementKeepsStateReferencesBalanced() {
        ThermalCellArena arena = arena(
                new double[]{10_000.0D, 0.0D},
                new double[]{1_000.0D, 1_000.0D});
        ThermalSweep sweep = materialSweep(arena, 1.0D);

        for (int replacement = 2; replacement <= 4; replacement++) {
            sweep = replaceMaterial(sweep, 0, replacement);
            assertEquals(1, sweep.pairOperationCount());
            assertEquals(2, sweep.stateCellCount());
        }

        assertEquals(1, sweep.apply(0.0D, epoch(1L)).appliedPairs());
    }

    @Test
    void buoyancyUsesTheCurrentArenaTemperatures() {
        BuoyancyConductance.Parameters parameters =
                new BuoyancyConductance.Parameters(0.25D, 4.0D, 20.0D);
        double[] capacities = {1_000.0D, 1_000.0D};
        ThermalCellArena hotArena = arena(
                new double[]{40_000.0D, 0.0D}, capacities);
        ThermalCellArena coldArena = arena(
                new double[]{0.0D, 40_000.0D}, capacities);
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
        ThermalSweepFragments.Builder builder = ThermalSweepFragments.builder(
                arena, null, NEUTRAL_BUOYANCY, 1);
        assertThrows(IllegalArgumentException.class, () -> builder.setAirPairs(
                0, List.of(ThermalSweep.PairOperation.fixed(0, 2, 10.0D))));
        assertThrows(IllegalArgumentException.class,
                () -> ThermalSweep.PairOperation.fixed(0, 1, -1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new ThermalSweep.BoundaryOperation(0, Double.NaN, 1.0D));
    }

    @Test
    void reusedArenaSlotsInvalidateTheWholeSweepBeforeMutation() {
        ThermalCellArena arena = new ThermalCellArena(0);
        ArenaSpan original = arena.allocatePageCells(
                0,
                1,
                new ThermalCellArena.CellSpec[]{
                        new ThermalCellArena.CellSpec(0, 0, 0, 0, 0, 64.0D),
                        new ThermalCellArena.CellSpec(4, 0, 0, 0, 0, 64.0D)},
                new ThermalCellArena.MixedBrickSpec[0],
                new ThermalCellArena.MaterialPoleSpec[0],
                new ThermalCellArena.PhaseReservoirSpec[0],
                0.0D,
                0.0D).cellSpan();
        int first = original.firstSlot();
        ThermalSweep stale = airSweep(
                arena,
                List.of(ThermalSweep.PairOperation.fixed(
                        first, first + 1, 10.0D)),
                List.of(),
                NEUTRAL_BUOYANCY);
        arena.releasePageCells(0, 1, original);
        ArenaSpan reused = arena.allocatePageCells(
                1,
                2,
                new ThermalCellArena.CellSpec[]{
                        new ThermalCellArena.CellSpec(16, 0, 0, 0, 0, 64.0D),
                        new ThermalCellArena.CellSpec(20, 0, 0, 0, 0, 64.0D)},
                new ThermalCellArena.MixedBrickSpec[0],
                new ThermalCellArena.MaterialPoleSpec[0],
                new ThermalCellArena.PhaseReservoirSpec[0],
                0.0D,
                0.0D).cellSpan();
        arena.setEnthalpyJ(reused.firstSlot(), 11.0D);
        arena.setEnthalpyJ(reused.firstSlot() + 1, 22.0D);
        assertEquals(first, reused.firstSlot());

        assertThrows(IllegalStateException.class, () ->
                stale.apply(0.0D, epoch(1L)));
        assertEquals(11.0D, arena.enthalpyJ(first), EPSILON);
        assertEquals(22.0D, arena.enthalpyJ(first + 1), EPSILON);
    }

    @Test
    void invalidFarBoundaryRejectsPatchBeforeReplacingAirPairs() {
        ThermalCellArena arena = arena(
                new double[]{10_000.0D, 0.0D},
                new double[]{1_000.0D, 1_000.0D});
        ThermalSweep sweep = airSweep(
                arena,
                List.of(ThermalSweep.PairOperation.fixed(0, 1, 10.0D)),
                List.of(),
                NEUTRAL_BUOYANCY);
        ThermalSweepFragments.Patch patch = sweep.beginFragmentPatch();
        patch.replaceAirPairs(0, List.of());
        patch.setFarBoundary(10_000, 0.0D, 1.0D);
        ThermalSweep pending = sweep.withFragmentPatch(patch);

        assertThrows(IllegalArgumentException.class,
                pending::commitPendingFragmentPatch);
        assertEquals(1, sweep.pairOperationCount());

        ThermalSweep.Result result = sweep.apply(0.0D, epoch(1L));
        assertEquals(1, result.appliedPairs());
        assertTrue(arena.enthalpyJ(0) < 10_000.0D);
        assertTrue(arena.enthalpyJ(1) > 0.0D);
    }

    private static ThermalSweep airSweep(
            ThermalCellArena arena,
            List<ThermalSweep.PairOperation> pairs,
            List<ThermalSweep.BoundaryOperation> boundaries,
            BuoyancyConductance.Parameters parameters
    ) {
        ThermalSweepFragments.Builder builder = ThermalSweepFragments.builder(
                arena, null, parameters, 1);
        builder.setAirPairs(0, pairs);
        builder.setMaterial(0, List.of(), boundaries, List.of());
        return builder.build();
    }

    private static ThermalSweep buoyantSweep(
            ThermalCellArena arena,
            BuoyancyConductance.Parameters parameters
    ) {
        return airSweep(
                arena,
                List.of(ThermalSweep.PairOperation.buoyant(
                        0, 1, 10.0D, 0.0D, 4.0D)),
                List.of(),
                parameters);
    }

    private static ThermalSweep materialSweep(
            ThermalCellArena arena,
            double... conductances
    ) {
        ThermalSweepFragments.Builder builder = ThermalSweepFragments.builder(
                arena, null, NEUTRAL_BUOYANCY, conductances.length);
        for (int fragment = 0; fragment < conductances.length; fragment++) {
            builder.setMaterial(
                    fragment,
                    List.of(ThermalSweep.PairOperation.fixed(
                            0, 1, conductances[fragment])),
                    List.of(),
                    List.of());
        }
        return builder.build();
    }

    private static ThermalSweep replaceMaterial(
            ThermalSweep sweep,
            int fragment,
            double conductance
    ) {
        ThermalSweepFragments.Patch patch = sweep.beginFragmentPatch();
        patch.replaceMaterial(
                fragment,
                List.of(ThermalSweep.PairOperation.fixed(0, 1, conductance)),
                List.of(),
                List.of());
        ThermalSweep patched = sweep.withFragmentPatch(patch);
        patched.commitPendingFragmentPatch();
        return patched;
    }

    private static SolveEpoch epoch(long id) {
        return new SolveEpoch(0L, 5L, id, 1L, InputWatermarks.ZERO);
    }

    private static ThermalCellArena arena(double[] enthalpies, double[] capacities) {
        ThermalCellArena.CellSpec[] cells =
                new ThermalCellArena.CellSpec[enthalpies.length];
        for (int index = 0; index < cells.length; index++) {
            int x = (index & 3) * 4;
            int z = ((index >>> 2) & 3) * 4;
            int y = ((index >>> 4) & 3) * 4;
            cells[index] = new ThermalCellArena.CellSpec(
                    x, y, z, 0, 0, capacities[index]);
        }
        ThermalCellArena arena = new ThermalCellArena(0);
        ThermalCellArena.PageAllocation allocation = arena.allocatePageCells(
                0,
                1,
                cells,
                new ThermalCellArena.MixedBrickSpec[0],
                new ThermalCellArena.MaterialPoleSpec[0],
                new ThermalCellArena.PhaseReservoirSpec[0],
                0.0D,
                0.0D);
        for (int index = 0; index < enthalpies.length; index++) {
            arena.setEnthalpyJ(allocation.cellSpan().firstSlot() + index, enthalpies[index]);
        }
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
