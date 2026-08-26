/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.solver.InputWatermarks;
import com.teammoeg.frostedheart.content.climate.thermal.solver.LatestSolveEpochScheduler;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SealedInputFrame;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalStepExecutor;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweepFragments;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalTimePolicy;
import com.teammoeg.frostedheart.content.climate.thermal.source.EmissionPort;
import com.teammoeg.frostedheart.content.climate.thermal.source.NodePowerAccumulatorArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceTimeline;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DimensionThermalRuntimeTest {
    private static final double EPSILON = 1.0e-9D;

    @Test
    void explicitNonSourceAckUnblocksTheAlreadyStartedEpoch() {
        Fixture fixture = fixture(1, limits(8, 8, 8, 3));
        InputWatermarks sealed = new InputWatermarks(1L, 0L, 0L, 0L, 0L);
        assertEquals(LatestSolveEpochScheduler.SealResult.ACCEPTED,
                fixture.runtime().sealFrame(new SealedInputFrame(5L, 9L, sealed)));

        fixture.runtime().runOne();
        assertEquals(0L, fixture.runtime().lastCompletedTargetTick());
        assertEquals(DimensionThermalRuntime.AcknowledgeResult.APPLIED,
                acknowledge(fixture.runtime(),
                        9L, sealed, 2L, 2L, true));

        fixture.runtime().runOne();
        assertEquals(5L, fixture.runtime().lastCompletedTargetTick());
        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        assertTrue(fixture.publication().tryRead(0, 9L, 2L, out));
        assertEquals(5L, out.sampleTick());
    }

    @Test
    void sourceIntegrationSweepAndPublicationUseTheSameArena() {
        Fixture fixture = fixture(1, limits(8, 8, 8, 3));
        long sourceWatermark = fixture.sources().offerRegister(
                100L,
                1,
                ThermalSourceMode.POWER_SOURCE,
                25.0D,
                true,
                0L,
                new EmissionPort[]{EmissionPort.of(
                        0,
                        1.0D,
                        SourceBinding.thermalNode(0L, 1))});
        InputWatermarks sealed = InputWatermarks.ZERO.withSource(sourceWatermark);
        fixture.runtime().sealFrame(new SealedInputFrame(5L, 9L, sealed));

        fixture.runtime().runOne();

        assertEquals(6.25D, fixture.arena().enthalpyJ(0), EPSILON);
        assertEquals(6.25D, fixture.sources().routedEnergyJ(
                100L, SourceBinding.Kind.THERMAL_NODE), EPSILON);
        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        assertTrue(fixture.publication().tryRead(0, 9L, 1L, out));
        assertEquals(0.0625D, out.temperatureC(), EPSILON);
        assertFalse(fixture.runtime().sleeping());
    }

    @Test
    void latestSealedTargetCoalescesBeforeExecution() {
        Fixture fixture = fixture(1, limits(8, 8, 8, 3));
        fixture.runtime().sealFrame(new SealedInputFrame(
                5L, 9L, InputWatermarks.ZERO));
        fixture.runtime().sealFrame(new SealedInputFrame(
                15L, 9L, InputWatermarks.ZERO));

        fixture.runtime().runOne();

        assertEquals(15L, fixture.runtime().lastCompletedTargetTick());
        fixture.runtime().runOne();
        assertEquals(15L, fixture.runtime().lastCompletedTargetTick());
    }

    @Test
    void wholeSolveSetSleepsConservativelyAndWatermarkAdvanceWakesIt() {
        Fixture fixture = fixture(1, limits(8, 8, 8, 2));
        fixture.runtime().sealFrame(new SealedInputFrame(
                5L, 9L, InputWatermarks.ZERO));
        fixture.runtime().runOne();
        assertFalse(fixture.runtime().sleeping());

        fixture.runtime().sealFrame(new SealedInputFrame(
                10L, 9L, InputWatermarks.ZERO));
        fixture.runtime().runOne();
        assertTrue(fixture.runtime().sleeping());

        fixture.runtime().sealFrame(new SealedInputFrame(
                15L, 9L, InputWatermarks.ZERO));
        fixture.runtime().runOne();
        assertTrue(fixture.runtime().sleeping());
        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        assertTrue(fixture.publication().tryRead(0, 9L, 1L, out));
        assertEquals(15L, out.sampleTick());

        InputWatermarks changed = new InputWatermarks(1L, 0L, 0L, 0L, 0L);
        acknowledge(fixture.runtime(),
                9L, changed, 2L, 2L, true);
        fixture.runtime().sealFrame(new SealedInputFrame(20L, 9L, changed));
        fixture.runtime().runOne();
        assertFalse(fixture.runtime().sleeping());
    }

    @Test
    void stableUnresolvedTopologyCanSleepUntilNewEvidenceArrives() {
        Fixture fixture = fixture(1, limits(8, 8, 8, 2), false);
        fixture.runtime().sealFrame(new SealedInputFrame(
                5L, 9L, InputWatermarks.ZERO));
        fixture.runtime().runOne();
        assertFalse(fixture.runtime().sleeping());
        fixture.runtime().sealFrame(new SealedInputFrame(
                10L, 9L, InputWatermarks.ZERO));

        fixture.runtime().runOne();
        assertTrue(fixture.runtime().sleeping());
        assertFalse(fixture.runtime().topologyResolved());
    }

    @Test
    void publicationGrowsToCoverArenaAdmissionBeforePublishing() {
        Fixture fixture = fixture(1, limits(8, 8, 8, 2));
        fixture.arena().allocatePageCells(
                1,
                1,
                new ThermalCellArena.CellSpec[]{new ThermalCellArena.CellSpec(
                        4, 0, 0, 0, 0, 100.0D)},
                new ThermalCellArena.MixedBrickSpec[0],
                new ThermalCellArena.MaterialPoleSpec[0],
                new ThermalCellArena.PhaseReservoirSpec[0],
                0.0D,
                0.0D);
        assertEquals(1, fixture.publication().capacity());
        fixture.runtime().sealFrame(new SealedInputFrame(
                5L, 9L, InputWatermarks.ZERO));

        fixture.runtime().runOne();

        assertTrue(fixture.publication().capacity() >= fixture.arena().highWaterMark());
        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        assertTrue(fixture.publication().tryRead(1, 9L, 1L, out));
    }

    @Test
    void hardWorkCapRefusesAnOversizedCompleteSweep() {
        Fixture fixture = fixture(2, limits(1, 8, 8, 2));
        fixture.runtime().sealFrame(new SealedInputFrame(
                5L, 9L, InputWatermarks.ZERO));

        fixture.runtime().runOne();

        assertEquals(0L, fixture.runtime().lastCompletedTargetTick());
        assertTrue(fixture.runtime().inFlightTopologyRecoveryRequired());
        RuntimeException failure = fixture.runtime().consumeFailureForRecovery();
        assertTrue(failure instanceof DimensionThermalRuntime.WorkLimitExceededException);
        assertEquals(
                new SealedInputFrame(5L, 9L, InputWatermarks.ZERO),
                fixture.runtime().inFlightFrame().orElseThrow());
        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        assertFalse(fixture.publication().tryRead(0, 9L, 1L, out));
    }

    @Test
    void unloadRetiresPublicationAndRejectsTheOldGeneration() {
        Fixture fixture = fixture(1, limits(8, 8, 8, 2));
        fixture.runtime().sealFrame(new SealedInputFrame(
                5L, 9L, InputWatermarks.ZERO));
        fixture.runtime().runOne();
        fixture.runtime().unload();

        fixture.runtime().runOne();
        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        assertFalse(fixture.publication().tryRead(0, 9L, 1L, out));
    }

    @Test
    void productionReaderRejectsAFormerPublicationAfterTopologyAdvance() {
        Fixture fixture = fixture(1, limits(8, 8, 8, 2));
        fixture.runtime().sealFrame(new SealedInputFrame(
                5L, 9L, InputWatermarks.ZERO));
        fixture.runtime().runOne();

        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        assertTrue(fixture.runtime().tryReadPublishedCell(0, out));
        assertEquals(5L, out.sampleTick());

        InputWatermarks changed = new InputWatermarks(1L, 0L, 0L, 0L, 0L);
        assertEquals(DimensionThermalRuntime.AcknowledgeResult.APPLIED,
                acknowledge(fixture.runtime(),
                        9L, changed, 2L, 2L, true));
        assertFalse(fixture.runtime().tryReadPublishedCell(0, out));
    }

    @Test
    void failedSweepInvalidatesPublicationAndRetriesWithoutReapplyingSources() {
        ThermalCellArena arena = new ThermalCellArena(1);
        ThermalCellArena.PageAllocation allocation = arena.allocatePageCells(
                0,
                1,
                new ThermalCellArena.CellSpec[]{new ThermalCellArena.CellSpec(
                        0, 0, 0, 0, 0, 100.0D)},
                new ThermalCellArena.MixedBrickSpec[0],
                new ThermalCellArena.MaterialPoleSpec[0],
                new ThermalCellArena.PhaseReservoirSpec[0],
                0.0D,
                0.0D);
        NodePowerAccumulatorArena accumulators = new NodePowerAccumulatorArena(0);
        ThermalSourceTimeline sources = new ThermalSourceTimeline(
                9L,
                0L,
                16,
                new ThermalSourceRegistry(1, 2, accumulators),
                arena);
        ThermalSweepFragments.Builder failingBuilder = ThermalSweepFragments.builder(
                arena,
                null,
                new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D),
                0);
        failingBuilder.setFarBoundary(0, 0.0D, 1.0D);
        ThermalMemoryBudget server = new ThermalMemoryBudget(1_000_000L, 0L);
        QueryPublication publication = QueryPublication.tryCreate(
                server.createDimensionBudget(1_000_000L, 0L), 1);
        assertNotNull(publication);
        DimensionThermalRuntime runtime = new DimensionThermalRuntime(
                9L,
                0L,
                InputWatermarks.ZERO,
                1L,
                1L,
                true,
                new ThermalTimePolicy(5L, 20L, 2),
                arena,
                sources,
                failingBuilder.build(),
                publication,
                0.0D,
                limits(8, 8, 8, 2));

        runtime.sealFrame(new SealedInputFrame(5L, 9L, InputWatermarks.ZERO));
        runtime.runOne();
        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        assertTrue(publication.tryRead(0, 9L, 1L, out));

        long sourceWatermark = sources.offerRegister(
                7L,
                1,
                ThermalSourceMode.POWER_SOURCE,
                20.0D,
                true,
                5L,
                new EmissionPort[]{EmissionPort.of(
                        0, 1.0D, SourceBinding.declaredLoss(1L))});
        arena.releasePageCells(0, 1, allocation.cellSpan());
        runtime.sealFrame(new SealedInputFrame(
                45L, 9L, InputWatermarks.ZERO.withSource(sourceWatermark)));
        runtime.runOne();

        assertFalse(publication.tryRead(0, 9L, 1L, out));
        assertEquals(5L, sources.cursorTick());
        assertFalse(runtime.tryBeginTopologyUpdate());
        long reboundWatermark = sources.offerRebind(
                7L, 0, SourceBinding.declaredLoss(2L), 50L);
        SealedInputFrame newerFrame = new SealedInputFrame(
                50L,
                9L,
                InputWatermarks.ZERO.withSource(reboundWatermark));
        assertEquals(
                LatestSolveEpochScheduler.SealResult.ACCEPTED,
                runtime.sealFrame(newerFrame));
        RuntimeException failure = runtime.consumeFailureForRecovery();
        assertNotNull(failure);
        assertTrue(failure instanceof ThermalStepExecutor.ExecutionFailure);
        assertTrue(((ThermalStepExecutor.ExecutionFailure) failure)
                .failedBeforeMutation());
        assertEquals(
                new SealedInputFrame(
                        45L,
                        9L,
                        InputWatermarks.ZERO.withSource(sourceWatermark)),
                runtime.inFlightFrame().orElseThrow());
        assertTrue(runtime.tryBeginTopologyUpdate());
        ThermalSweep replacement = ThermalSweepFragments.builder(
                arena,
                null,
                new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D),
                0).build();
        assertEquals(DimensionThermalRuntime.AcknowledgeResult.APPLIED,
                runtime.finishTopologyUpdate(
                        9L,
                        InputWatermarks.ZERO,
                        2L,
                        2L,
                        true,
                        replacement));
        runtime.completeInFlightTopologyRecovery();

        runtime.runOne();

        assertEquals(45L, runtime.lastCompletedTargetTick());
        assertEquals(45L, sources.cursorTick());
        assertEquals(40.0D, sources.routedEnergyJ(
                7L, SourceBinding.Kind.DECLARED_LOSS), EPSILON);

        runtime.runOne();
        assertEquals(50L, runtime.lastCompletedTargetTick());
        assertEquals(45.0D, sources.routedEnergyJ(
                7L, SourceBinding.Kind.DECLARED_LOSS), EPSILON);
    }

    private static DimensionThermalRuntime.Limits limits(
            int cells,
            int pairs,
            int boundaries,
            int sleepEpochs
    ) {
        return new DimensionThermalRuntime.Limits(
                cells, pairs, boundaries, sleepEpochs, 1.0e-9D);
    }

    private static DimensionThermalRuntime.AcknowledgeResult acknowledge(
            DimensionThermalRuntime runtime,
            long generation,
            InputWatermarks watermarks,
            long geometryRevision,
            long topologyGeneration,
            boolean topologyResolved
    ) {
        assertTrue(runtime.tryBeginTopologyUpdate());
        return runtime.finishTopologyUpdate(
                generation,
                watermarks,
                geometryRevision,
                topologyGeneration,
                topologyResolved,
                null);
    }

    private static Fixture fixture(
            int cellCount,
            DimensionThermalRuntime.Limits limits
    ) {
        return fixture(cellCount, limits, true);
    }

    private static Fixture fixture(
            int cellCount,
            DimensionThermalRuntime.Limits limits,
            boolean topologyResolved
    ) {
        ThermalCellArena.CellSpec[] cells = new ThermalCellArena.CellSpec[cellCount];
        for (int slot = 0; slot < cellCount; slot++) {
            cells[slot] = new ThermalCellArena.CellSpec(
                    slot * 4, 0, 0, 0, 0, 100.0D);
        }
        ThermalCellArena arena = new ThermalCellArena(cellCount);
        arena.allocatePageCells(
                0,
                1,
                cells,
                new ThermalCellArena.MixedBrickSpec[0],
                new ThermalCellArena.MaterialPoleSpec[0],
                new ThermalCellArena.PhaseReservoirSpec[0],
                0.0D,
                0.0D);
        NodePowerAccumulatorArena accumulators = new NodePowerAccumulatorArena(0);
        ThermalSourceTimeline sources = new ThermalSourceTimeline(
                9L,
                0L,
                16,
                new ThermalSourceRegistry(0, 2, accumulators),
                arena);
        ThermalSweep sweep = ThermalSweepFragments.builder(
                arena, null,
                new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D), 0).build();
        ThermalMemoryBudget server = new ThermalMemoryBudget(1_000_000L, 0L);
        QueryPublication publication = QueryPublication.tryCreate(
                server.createDimensionBudget(1_000_000L, 0L),
                Math.max(1, arena.highWaterMark()));
        assertNotNull(publication);
        DimensionThermalRuntime runtime = new DimensionThermalRuntime(
                9L,
                0L,
                InputWatermarks.ZERO,
                1L,
                1L,
                topologyResolved,
                new ThermalTimePolicy(5L, 20L, 2),
                arena,
                sources,
                sweep,
                publication,
                0.0D,
                limits);
        return new Fixture(runtime, arena, sources, publication);
    }

    private record Fixture(
            DimensionThermalRuntime runtime,
            ThermalCellArena arena,
            ThermalSourceTimeline sources,
            QueryPublication publication
    ) {
    }
}
