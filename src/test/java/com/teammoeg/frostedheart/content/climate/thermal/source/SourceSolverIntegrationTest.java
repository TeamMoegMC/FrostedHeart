/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.solver.InputWatermarks;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SolveEpoch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalStepExecutor;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalStepPlan;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweepFragments;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalTimePolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceSolverIntegrationTest {
    private static final double EPSILON = 1.0e-9D;
    @Test
    void degradedTimeAppliesTheCompleteSealedSourceTimelineOnce() {
        Fixture fixture = fixture(2);
        ThermalSourceTimeline timeline = fixture.timeline();
        timeline.offerRegister(
                100L, 1, ThermalSourceMode.POWER_SOURCE,
                100.0D, true, 0L,
                new EmissionPort[]{nodePort(1, 0L, 1)});
        long sealedSourceWatermark = timeline.offerRebind(
                100L, 1, SourceBinding.thermalNode(1L, 1), 15L);

        SolveEpoch epoch = epoch(0L, 35L, 1L, sealedSourceWatermark);
        ThermalStepPlan plan = new ThermalTimePolicy(5L, 10L, 2).plan(epoch);
        ThermalStepExecutor.Report report = execute(plan, fixture);

        assertEquals(ThermalStepExecutor.Status.COMPLETED, report.status());
        assertEquals(ThermalStepPlan.Status.TIME_DEGRADED, report.timeStatus());
        assertEquals(15L, report.skippedTransportTicks());
        assertEquals(15L, report.skippedPhaseTicks());
        assertEquals(175.0D, report.sourceAppliedJ(), EPSILON);
        assertEquals(75.0D, fixture.arena().enthalpyJ(0), EPSILON);
        assertEquals(100.0D, fixture.arena().enthalpyJ(1), EPSILON);
        assertEquals(sealedSourceWatermark, timeline.appliedWatermark());
        assertEquals(175.0D, timeline.routedEnergyJ(
                100L, SourceBinding.Kind.THERMAL_NODE), EPSILON);
    }

    @Test
    void sourceEnergyEntersTheSameEnthalpyStateSweptByTransport() {
        ThermalCellArena arena = arena(2);
        NodePowerAccumulatorArena nodes = new NodePowerAccumulatorArena(0);
        ThermalSourceTimeline timeline = new ThermalSourceTimeline(
                1L,
                0L,
                8,
                new ThermalSourceRegistry(0, 1, nodes),
                arena
        );
        long sourceWatermark = timeline.offerRegister(
                150L, 1, ThermalSourceMode.POWER_SOURCE,
                200.0D, true, 0L,
                new EmissionPort[]{nodePort(1, 0L, 1)});
        ThermalStepPlan plan = new ThermalTimePolicy(5L, 10L, 1)
                .plan(epoch(0L, 10L, 1L, sourceWatermark));
        ThermalSweepFragments.Builder sweepBuilder = ThermalSweepFragments.builder(
                arena, null,
                new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D), 1);
        sweepBuilder.setAirPairs(
                0, List.of(ThermalSweep.PairOperation.fixed(0, 1, 10.0D)));
        ThermalSweep sweep = sweepBuilder.build();

        ThermalStepExecutor.Report report = ThermalStepExecutor.execute(
                plan,
                1L,
                plan.epoch().sealedWatermarks().withSource(0L),
                arena,
                timeline,
                sweep,
                0.0D
        );

        assertEquals(ThermalStepExecutor.Status.COMPLETED, report.status());
        assertEquals(100.0D, report.sourceAppliedJ(), EPSILON);
        assertEquals(100.0D, arena.enthalpyJ(0) + arena.enthalpyJ(1), EPSILON);
        assertTrue(arena.enthalpyJ(0) > arena.enthalpyJ(1));
        assertTrue(arena.enthalpyJ(1) > 0.0D);
        assertEquals(sourceWatermark, report.appliedWatermarks().source());
    }

    @Test
    void unavailableSealedWatermarkDoesNotMutateSourceState() {
        Fixture fixture = fixture(1);
        ThermalSourceTimeline timeline = fixture.timeline();
        long availableWatermark = timeline.offerRegister(
                200L, 1, ThermalSourceMode.POWER_SOURCE,
                100.0D, true, 0L,
                new EmissionPort[]{nodePort(1, 0L, 1)});
        SolveEpoch epoch = epoch(0L, 5L, 1L, availableWatermark + 1L);

        ThermalStepExecutor.Report report = execute(
                new ThermalTimePolicy(5L, 10L, 1).plan(epoch), fixture);

        assertEquals(ThermalStepExecutor.Status.INPUTS_PENDING, report.status());
        assertEquals(0L, timeline.appliedWatermark());
        assertEquals(0.0D, report.sourceAppliedJ(), EPSILON);
        assertEquals(0.0D, fixture.arena().enthalpyJ(0), EPSILON);
        assertEquals(0L, report.appliedWatermarks().source());
    }

    @Test
    void futureSourceCommandCannotAdvanceTheCurrentEpoch() {
        Fixture fixture = fixture(1);
        ThermalSourceTimeline timeline = fixture.timeline();
        long registrationWatermark = timeline.offerRegister(
                300L, 1, ThermalSourceMode.POWER_SOURCE,
                100.0D, true, 0L,
                new EmissionPort[]{nodePort(1, 0L, 1)});
        long futureWatermark = timeline.offerPowerChange(300L, 200.0D, 40L);

        ThermalStepExecutor.Report first = execute(
                new ThermalTimePolicy(5L, 20L, 1)
                        .plan(epoch(0L, 20L, 1L, registrationWatermark)), fixture);
        assertEquals(ThermalStepExecutor.Status.COMPLETED, first.status());
        assertEquals(100.0D, first.sourceAppliedJ(), EPSILON);
        assertEquals(registrationWatermark, timeline.appliedWatermark());
        ThermalStepExecutor.Report second = execute(
                new ThermalTimePolicy(5L, 20L, 1)
                        .plan(epoch(20L, 40L, 2L, futureWatermark)), fixture);
        assertEquals(ThermalStepExecutor.Status.COMPLETED, second.status());
        assertEquals(100.0D, second.sourceAppliedJ(), EPSILON);
        assertEquals(200.0D, fixture.arena().enthalpyJ(0), EPSILON);
        assertEquals(futureWatermark, timeline.appliedWatermark());
    }

    @Test
    void signedImpulsesUseTheBindingAtTheirEventOrder() {
        Fixture fixture = fixture(2);
        ThermalSourceTimeline timeline = fixture.timeline();
        timeline.offerRegister(
                400L, 1, ThermalSourceMode.IMPULSE,
                0.0D, true, 0L,
                new EmissionPort[]{nodePort(7, 0L, 1)});
        timeline.offerImpulse(400L, 7, 10.0D, 10L);
        timeline.offerRebind(
                400L, 7, SourceBinding.thermalNode(1L, 1), 10L);
        long sealedSourceWatermark = timeline.offerImpulse(
                400L, 7, -20.0D, 10L);

        ThermalStepExecutor.Report report = execute(
                new ThermalTimePolicy(5L, 10L, 1)
                        .plan(epoch(0L, 10L, 1L, sealedSourceWatermark)), fixture);

        assertEquals(ThermalStepExecutor.Status.COMPLETED, report.status());
        assertEquals(-10.0D, report.sourceAppliedJ(), EPSILON);
        assertEquals(10.0D, fixture.arena().enthalpyJ(0), EPSILON);
        assertEquals(-20.0D, fixture.arena().enthalpyJ(1), EPSILON);
    }

    @Test
    void fullTimelineRejectsWithoutCreatingAWatermarkGap() {
        ThermalCellArena arena = arena(1);
        NodePowerAccumulatorArena nodes = new NodePowerAccumulatorArena(0);
        ThermalSourceTimeline timeline = new ThermalSourceTimeline(
                1L,
                0L,
                1,
                new ThermalSourceRegistry(0, 1, nodes),
                arena
        );
        long registrationWatermark = timeline.offerRegister(
                500L, 1, ThermalSourceMode.POWER_SOURCE,
                100.0D, true, 0L,
                new EmissionPort[]{nodePort(1, 0L, 1)});

        assertEquals(ThermalSourceTimeline.OFFER_REJECTED,
                timeline.offerPowerChange(500L, 200.0D, 5L));
        ThermalStepExecutor.Report first = execute(
                new ThermalTimePolicy(5L, 5L, 1)
                        .plan(epoch(0L, 5L, 1L, registrationWatermark)),
                new Fixture(arena, timeline)
        );
        assertEquals(ThermalStepExecutor.Status.COMPLETED, first.status());

        long retriedWatermark = timeline.offerPowerChange(500L, 200.0D, 5L);
        assertEquals(registrationWatermark + 1L, retriedWatermark);
        ThermalStepExecutor.Report retry = execute(
                new ThermalTimePolicy(5L, 5L, 1)
                        .plan(epoch(5L, 5L, 2L, retriedWatermark)),
                new Fixture(arena, timeline)
        );
        assertEquals(ThermalStepExecutor.Status.COMPLETED, retry.status());
        assertEquals(retriedWatermark, timeline.appliedWatermark());
    }

    @Test
    void executorRejectsSourceAndSweepBoundToDifferentArenas() {
        ThermalCellArena sourceArena = arena(1);
        ThermalCellArena sweepArena = arena(1);
        ThermalSourceTimeline timeline = new ThermalSourceTimeline(
                1L,
                0L,
                8,
                new ThermalSourceRegistry(
                        0, 1, new NodePowerAccumulatorArena(0)),
                sourceArena);
        ThermalStepPlan plan = new ThermalTimePolicy(5L, 10L, 1)
                .plan(epoch(0L, 0L, 1L, 0L));

        assertThrows(IllegalArgumentException.class, () ->
                ThermalStepExecutor.execute(
                        plan,
                        1L,
                        plan.epoch().sealedWatermarks(),
                        sourceArena,
                        timeline,
                        emptySweep(sweepArena),
                        0.0D));
    }

    private static Fixture fixture(int cellCount) {
        ThermalCellArena arena = arena(cellCount);
        NodePowerAccumulatorArena nodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry registry = new ThermalSourceRegistry(0, 2, nodes);
        return new Fixture(arena, new ThermalSourceTimeline(
                1L,
                0L,
                16,
                registry,
                arena));
    }

    private static ThermalStepExecutor.Report execute(
            ThermalStepPlan plan,
            Fixture fixture
    ) {
        return ThermalStepExecutor.execute(
                plan,
                plan.epoch().dimensionGeneration(),
                plan.epoch().sealedWatermarks(),
                fixture.arena(),
                fixture.timeline(),
                emptySweep(fixture.arena()),
                0.0D
        );
    }

    private static ThermalCellArena arena(int cellCount) {
        ThermalCellArena.CellSpec[] cells = new ThermalCellArena.CellSpec[cellCount];
        double[] enthalpies = new double[cellCount];
        for (int slot = 0; slot < cellCount; slot++) {
            cells[slot] = new ThermalCellArena.CellSpec(
                    (slot & 3) * 4,
                    ((slot >>> 4) & 3) * 4,
                    ((slot >>> 2) & 3) * 4,
                    0, 0, 100.0D);
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

    private static ThermalSweep emptySweep(ThermalCellArena arena) {
        return ThermalSweepFragments.builder(
                arena, null,
                new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D), 0).build();
    }

    private static SolveEpoch epoch(
            long previousTick,
            long targetTick,
            long epochId,
            long sourceWatermark
    ) {
        return new SolveEpoch(
                previousTick,
                targetTick,
                epochId,
                1L,
                new InputWatermarks(1L, sourceWatermark, 1L, 1L, 1L)
        );
    }

    private static EmissionPort nodePort(int portId, long nodeId, int generation) {
        return EmissionPort.of(
                portId,
                1.0D,
                SourceBinding.thermalNode(nodeId, generation)
        );
    }

    private record Fixture(ThermalCellArena arena, ThermalSourceTimeline timeline) {
    }

}
