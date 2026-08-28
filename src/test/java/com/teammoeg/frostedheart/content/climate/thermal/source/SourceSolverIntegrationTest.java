/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.ThermalTestFixtures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalExchangeKernel;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalFragment;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceSolverIntegrationTest {
    private static final double EPSILON = 1.0e-9D;

    @Test
    void sourceDeliveryAndTransportUseTheSameArenaAuthority() {
        ThermalCellArena arena = new ThermalCellArena(2);
        int hot = ThermalTestFixtures.regularBrick(
                arena, 0, 1, 0, 0, 0,
                100.0D, 0.0D, 0.0D).cellSpan().firstSlot();
        int cold = ThermalTestFixtures.regularBrick(
                arena, 1, 1, 4, 0, 0,
                100.0D, 0.0D, 0.0D).cellSpan().firstSlot();
        ThermalSourceLedger sources = new ThermalSourceLedger(
                0L, 1, 1,
                new NodePowerAccumulatorArena(1), arena);
        ThermalSourceBatch.Builder sourceEvents = new ThermalSourceBatch.Builder(0L);
        sourceEvents.addRegister(
                1L, 1, ThermalSourceMode.POWER_SOURCE,
                20.0D, true, 0L,
                0, 0, 0, 1,
                new EmissionPort[]{EmissionPort.of(
                        0, 1.0D, SourceBinding.thermalNode(hot, 1))});
        sources.acceptAndAdvance(
                sourceEvents.buildAndReset(),
                20L,
                (events, index, ledger) -> { });

        ThermalSolver solver = solver(arena);
        double coefficient = ThermalExchangeKernel.compilePairCoefficientJPerK(
                100.0D, 100.0D, 10.0D, 1.0D);
        solver.installFragment(0, new ThermalFragment(
                1L,
                new ThermalFragment.AirPairs(
                        new int[]{hot}, new int[]{cold},
                        new int[]{1}, new int[]{1},
                        new double[]{10.0D}, new double[]{coefficient},
                        new double[]{2.0D}, new double[]{2.0D},
                        new byte[]{0}),
                ThermalFragment.MaterialContributions.EMPTY,
                ThermalFragment.FixedBoundaries.EMPTY,
                ThermalFragment.PhaseContacts.EMPTY,
                ThermalFragment.FarBoundaries.EMPTY));
        solver.finishTopologyCommit(1L);

        assertEquals(20.0D, arena.enthalpyJ(hot), EPSILON);
        assertEquals(ThermalSolver.StepStatus.COMPLETED,
                solver.step(1.0D, true));
        assertEquals(20.0D,
                arena.enthalpyJ(hot) + arena.enthalpyJ(cold), EPSILON);
        assertTrue(arena.enthalpyJ(cold) > 0.0D);
        assertTrue(arena.enthalpyJ(hot) < 20.0D);
    }

    private static ThermalSolver solver(ThermalCellArena arena) {
        return new ThermalSolver(
                arena,
                new PhaseTransitionRuntime(arena, 4),
                new BuoyancyConductance.Parameters(0.25D, 4.0D, 10.0D),
                0.0D,
                1,
                1,
                1);
    }
}
