/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.ThermalTestFixtures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalSolverTest {
    private static final double EPSILON = 1.0e-9D;

    @Test
    void compiledPairConservesEnergyAndMovesHeatTowardColdCell() {
        Fixture fixture = fixture(2);
        fixture.arena.setEnthalpyJ(fixture.slots[0], 1_000.0D);
        fixture.solver.installFragment(0, airPair(
                fixture, 0, 1, 10.0D));
        fixture.solver.finishTopologyCommit(1L);

        assertEquals(ThermalSolver.StepStatus.COMPLETED,
                fixture.solver.step(1.0D, true));
        assertEquals(1_000.0D,
                fixture.arena.enthalpyJ(fixture.slots[0])
                        + fixture.arena.enthalpyJ(fixture.slots[1]),
                EPSILON);
        assertTrue(fixture.arena.enthalpyJ(fixture.slots[0]) < 1_000.0D);
        assertTrue(fixture.arena.enthalpyJ(fixture.slots[1]) > 0.0D);
    }

    @Test
    void materialExecutionUsesItsOwnSparsePresenceIndex() {
        Fixture fixture = fixture(2);
        fixture.arena.setEnthalpyJ(fixture.slots[0], 500.0D);
        double coefficient = ThermalExchangeKernel.compilePairCoefficientJPerK(
                100.0D, 100.0D, 5.0D, 1.0D);
        fixture.solver.installMaterialExecution(
                3,
                new ThermalMaterialExecution(
                        new long[]{1L},
                        new int[]{fixture.slots[0]},
                        new int[]{fixture.slots[1]},
                        new double[]{5.0D},
                        new double[]{coefficient}));

        fixture.solver.step(1.0D, true);

        assertTrue(fixture.arena.enthalpyJ(fixture.slots[1]) > 0.0D);
        assertEquals(500.0D,
                fixture.arena.enthalpyJ(fixture.slots[0])
                        + fixture.arena.enthalpyJ(fixture.slots[1]),
                EPSILON);
    }

    @Test
    void fixedBoundaryUsesCompiledCoefficientAtOneSecond() {
        Fixture fixture = fixture(1);
        int cell = fixture.slots[0];
        double coefficient = ThermalExchangeKernel
                .compileBoundaryCoefficientJPerK(100.0D, 10.0D, 1.0D);
        fixture.solver.installFragment(0, new ThermalFragment(
                0L,
                ThermalFragment.AirPairs.EMPTY,
                ThermalFragment.MaterialContributions.EMPTY,
                new ThermalFragment.FixedBoundaries(
                        new int[]{cell}, new int[]{1},
                        new double[]{20.0D}, new double[]{10.0D},
                        new double[]{coefficient}),
                ThermalFragment.PhaseContacts.EMPTY,
                ThermalFragment.FarBoundaries.EMPTY));

        fixture.solver.step(1.0D, true);

        assertTrue(fixture.arena.enthalpyJ(cell) > 0.0D);
        assertTrue(fixture.arena.temperatureC(cell, 0.0D) < 20.0D);
    }

    @Test
    void invalidCompiledOperationDegradesWithoutMutatingEitherCell() {
        Fixture fixture = fixture(2);
        fixture.arena.setEnthalpyJ(fixture.slots[0], 100.0D);
        ThermalFragment broken = airPair(fixture, 0, 1, 1.0D);
        broken.airPairs().coefficient()[0] = Double.NaN;
        fixture.solver.installFragment(0, broken);

        assertEquals(ThermalSolver.StepStatus.NUMERIC_DEGRADED,
                fixture.solver.step(1.0D, true));
        assertEquals(100.0D,
                fixture.arena.enthalpyJ(fixture.slots[0]), EPSILON);
        assertEquals(0.0D,
                fixture.arena.enthalpyJ(fixture.slots[1]), EPSILON);
    }

    private static ThermalFragment airPair(
            Fixture fixture,
            int first,
            int second,
            double conductance
    ) {
        double coefficient = ThermalExchangeKernel.compilePairCoefficientJPerK(
                100.0D, 100.0D, conductance, 1.0D);
        return new ThermalFragment(
                0L,
                new ThermalFragment.AirPairs(
                        new int[]{fixture.slots[first]},
                        new int[]{fixture.slots[second]},
                        new int[]{1}, new int[]{1},
                        new double[]{conductance},
                        new double[]{coefficient},
                        new double[]{2.0D}, new double[]{2.0D},
                        new byte[]{0}),
                ThermalFragment.MaterialContributions.EMPTY,
                ThermalFragment.FixedBoundaries.EMPTY,
                ThermalFragment.PhaseContacts.EMPTY,
                ThermalFragment.FarBoundaries.EMPTY);
    }

    private static Fixture fixture(int count) {
        ThermalCellArena arena = new ThermalCellArena(count);
        int[] slots = new int[count];
        for (int index = 0; index < count; index++) {
            slots[index] = ThermalTestFixtures.regularBrick(
                    arena, index, 1, index * 4, 0, 0,
                    100.0D, 0.0D, 0.0D).cellSpan().firstSlot();
        }
        ThermalSolver solver = new ThermalSolver(
                arena,
                new PhaseTransitionRuntime(arena, 4),
                new BuoyancyConductance.Parameters(0.25D, 4.0D, 10.0D),
                0.0D,
                8,
                2,
                4);
        return new Fixture(arena, solver, slots);
    }

    private record Fixture(
            ThermalCellArena arena,
            ThermalSolver solver,
            int[] slots
    ) {
    }
}
