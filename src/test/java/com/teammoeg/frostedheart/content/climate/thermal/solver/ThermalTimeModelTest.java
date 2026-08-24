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

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.EmissionPort;
import com.teammoeg.frostedheart.content.climate.thermal.source.NodePowerAccumulatorArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceChannel;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceTimeline;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalTimeModelTest {
    private static final double EPSILON = 1.0e-12D;
    @Test
    void normalDelayedCadenceUsesOneUniformBoundedInterval() {
        ThermalTimePolicy policy = new ThermalTimePolicy(5L, 20L, 2);
        ThermalStepPlan plan = policy.plan(epoch(100L, 118L, 0L));

        assertEquals(ThermalStepPlan.Status.NORMAL, plan.status());
        assertEquals(1, plan.substepCount());
        assertEquals(100L, plan.substepStartTick(0));
        assertEquals(118L, plan.substepEndTick(0));
        assertEquals(0.9D, plan.substepDtSeconds(0), EPSILON);
        assertEquals(0L, plan.skippedTransportTicks());
        assertEquals(0L, plan.skippedPhaseTicks());
        assertEquals(18L, plan.sourceCoverageTicks());
    }

    @Test
    void overLimitDelayBoundsTheSweepAndRetainsAllSourceEnergy() {
        Fixture fixture = fixture(100L, 1);
        ThermalSourceTimeline sources = fixture.timeline();
        long sourceWatermark = sources.offerRegister(
                1L,
                0L,
                1,
                1,
                ThermalSourceMode.POWER_SOURCE,
                40.0D,
                true,
                100L,
                new EmissionPort[]{nodePort(0L)}
        );
        ThermalStepPlan plan = new ThermalTimePolicy(5L, 10L, 2)
                .plan(epoch(100L, 135L, sourceWatermark));

        ThermalStepExecutor.Report report = execute(plan, fixture);

        assertEquals(ThermalStepPlan.Status.TIME_DEGRADED, plan.status());
        assertEquals(2, plan.substepCount());
        assertEquals(20L, plan.coveredTransportTicks());
        assertEquals(15L, plan.skippedTransportTicks());
        assertEquals(15L, plan.skippedPhaseTicks());
        assertEquals(70.0D, report.sourceAppliedJ(), EPSILON);
        assertEquals(70.0D, fixture.arena().enthalpyJ(0), EPSILON);
        assertEquals(2, report.executedTransportSubsteps());
        assertEquals(ThermalStepExecutor.Status.COMPLETED, report.status());
        assertEquals(135L, sources.cursorTick());
    }

    @Test
    void unappliedWatermarkPreventsSourceAndSweepMutation() {
        InputWatermarks sealed = new InputWatermarks(3L, 7L, 2L, 1L, 4L);
        SolveEpoch epoch = new SolveEpoch(0L, 5L, 1L, 9L, sealed);
        ThermalStepPlan plan = new ThermalTimePolicy(5L, 10L, 1).plan(epoch);
        Fixture fixture = fixture(0L, 0);
        ThermalSourceTimeline sources = fixture.timeline();

        ThermalStepExecutor.Report report = ThermalStepExecutor.execute(
                plan,
                9L,
                new InputWatermarks(3L, 6L, 2L, 1L, 4L),
                fixture.arena(),
                sources,
                emptySweep(fixture.arena()),
                0.0D
        );

        assertEquals(ThermalStepExecutor.Status.INPUTS_PENDING, report.status());
        assertEquals(0, report.executedTransportSubsteps());
        assertEquals(0.0D, report.sourceAppliedJ());
        assertEquals(0L, sources.cursorTick());
        assertEquals(sealed, report.sealedWatermarks());
    }

    @Test
    void randomizedDelayPlansNeverExceedKernelBoundOrInventBacklogTime() {
        SplittableRandom random = new SplittableRandom(0x71C5EEDL);
        ThermalTimePolicy policy = new ThermalTimePolicy(5L, 17L, 3);
        for (int iteration = 0; iteration < 10_000; iteration++) {
            long duration = random.nextLong(0L, 10_000L);
            ThermalStepPlan plan = policy.plan(epoch(1_000L, 1_000L + duration, 0L));
            long covered = 0L;
            for (int step = 0; step < plan.substepCount(); step++) {
                assertTrue(plan.substepTicks(step) > 0L);
                assertTrue(plan.substepTicks(step) <= policy.maxSolveDeltaTicks());
                covered += plan.substepTicks(step);
            }
            assertEquals(duration, covered + plan.skippedTransportTicks());
            assertEquals(plan.skippedTransportTicks(), plan.skippedPhaseTicks());
            assertTrue(plan.substepCount() <= policy.maxDegradedSubsteps());
            if (duration <= policy.maxSolveDeltaTicks()) {
                assertEquals(ThermalStepPlan.Status.NORMAL, plan.status());
            } else {
                assertEquals(ThermalStepPlan.Status.TIME_DEGRADED, plan.status());
            }
        }
    }

    @Test
    void sweepNumericDegradationIsReportedByTheAggregateExecutor() {
        ThermalCellArena arena = arena(
                new double[]{Double.MAX_VALUE},
                new double[]{Double.MIN_NORMAL});
        ThermalSweep invalidSweep = new ThermalSweep(
                arena,
                List.of(),
                List.of(new ThermalSweep.BoundaryOperation(
                        0, 10.0D, 10.0D)),
                new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D)
        );
        ThermalStepPlan plan = new ThermalTimePolicy(5L, 10L, 1)
                .plan(epoch(0L, 5L, 0L));

        ThermalStepExecutor.Report report = ThermalStepExecutor.execute(
                plan,
                9L,
                plan.epoch().sealedWatermarks(),
                arena,
                timeline(arena, 0L),
                invalidSweep,
                0.0D
        );

        assertEquals(ThermalStepExecutor.Status.NUMERIC_DEGRADED, report.status());
        assertEquals(1, report.executedTransportSubsteps());
        assertEquals(Double.MAX_VALUE, arena.enthalpyJ(0));
    }

    @Test
    void invalidTimePolicyDomainsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ThermalTimePolicy(0L, 10L, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ThermalTimePolicy(10L, 5L, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ThermalTimePolicy(5L, 10L, 0));
    }

    @Test
    void zeroDurationWatermarkCutAppliesAnInstantaneousSourceEvent() {
        Fixture fixture = fixture(40L, 1);
        ThermalSourceTimeline sources = fixture.timeline();
        sources.offerRegister(
                2L,
                0L,
                1,
                1,
                ThermalSourceMode.IMPULSE,
                0.0D,
                true,
                40L,
                new EmissionPort[]{nodePort(0L)}
        );
        long sourceWatermark = sources.offerImpulse(2L, 0, 25.0D, 40L);
        ThermalStepPlan plan = new ThermalTimePolicy(5L, 10L, 1)
                .plan(epoch(40L, 40L, sourceWatermark));

        ThermalStepExecutor.Report report = execute(plan, fixture);

        assertEquals(ThermalStepExecutor.Status.COMPLETED, report.status());
        assertEquals(25.0D, report.sourceAppliedJ(), EPSILON);
        assertEquals(25.0D, fixture.arena().enthalpyJ(0), EPSILON);
        assertEquals(0, report.executedTransportSubsteps());
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

    private static Fixture fixture(
            long initialTick,
            int cellCount
    ) {
        ThermalCellArena arena = arena(
                new double[cellCount], filled(cellCount, 100.0D));
        return new Fixture(arena, timeline(arena, initialTick));
    }

    private static ThermalCellArena arena(double[] enthalpies, double[] capacities) {
        ThermalCellArena.CellSpec[] cells = new ThermalCellArena.CellSpec[enthalpies.length];
        for (int slot = 0; slot < cells.length; slot++) {
            cells[slot] = new ThermalCellArena.CellSpec(
                    (slot & 3) * 4,
                    ((slot >>> 4) & 3) * 4,
                    ((slot >>> 2) & 3) * 4,
                    4, 0, 0, capacities[slot]);
        }
        ThermalCellArena arena = new ThermalCellArena(0);
        arena.allocatePageCells(0, 1, cells, enthalpies);
        return arena;
    }

    private static double[] filled(int count, double value) {
        double[] values = new double[count];
        java.util.Arrays.fill(values, value);
        return values;
    }

    private static ThermalSourceTimeline timeline(
            ThermalCellArena arena,
            long initialTick
    ) {
        NodePowerAccumulatorArena nodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry registry = new ThermalSourceRegistry(0, 1, 8, nodes);
        return new ThermalSourceTimeline(9L, initialTick, 8, registry, arena);
    }

    private static ThermalSweep emptySweep(ThermalCellArena arena) {
        return new ThermalSweep(
                arena,
                List.of(),
                List.of(),
                new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D));
    }

    private static SolveEpoch epoch(
            long previousTick,
            long targetTick,
            long sourceWatermark
    ) {
        return new SolveEpoch(
                previousTick,
                targetTick,
                1L,
                9L,
                new InputWatermarks(3L, sourceWatermark, 5L, 6L, 7L)
        );
    }

    private static EmissionPort nodePort(long nodeId) {
        return EmissionPort.of(
                0,
                SourceChannel.CONVECTION,
                1.0D,
                SourceBinding.thermalNode(nodeId, 1)
        );
    }

    private record Fixture(ThermalCellArena arena, ThermalSourceTimeline timeline) {
    }

}
