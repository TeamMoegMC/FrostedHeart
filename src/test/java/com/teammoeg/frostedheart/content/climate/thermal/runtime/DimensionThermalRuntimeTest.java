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
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalTimePolicy;
import com.teammoeg.frostedheart.content.climate.thermal.source.EmissionPort;
import com.teammoeg.frostedheart.content.climate.thermal.source.NodePowerAccumulatorArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceChannel;
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

        assertEquals(DimensionThermalRuntime.RunStatus.INPUTS_PENDING,
                fixture.runtime().runOne().status());
        assertEquals(DimensionThermalRuntime.AcknowledgeResult.APPLIED,
                fixture.runtime().acknowledgeNonSourceInputs(
                        9L, sealed, 2L, 2L, true));

        DimensionThermalRuntime.RunReport completed = fixture.runtime().runOne();
        assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED, completed.status());
        assertTrue(completed.published());
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
                0L,
                1,
                1,
                ThermalSourceMode.POWER_SOURCE,
                25.0D,
                true,
                0L,
                new EmissionPort[]{EmissionPort.of(
                        0,
                        SourceChannel.CONVECTION,
                        1.0D,
                        SourceBinding.thermalNode(0L, 1))});
        InputWatermarks sealed = InputWatermarks.ZERO.withSource(sourceWatermark);
        fixture.runtime().sealFrame(new SealedInputFrame(5L, 9L, sealed));

        DimensionThermalRuntime.RunReport report = fixture.runtime().runOne();

        assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED, report.status());
        assertEquals(6.25D, report.thermalStep().sourceAppliedJ(), EPSILON);
        assertEquals(6.25D, fixture.arena().enthalpyJ(0), EPSILON);
        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        assertTrue(fixture.publication().tryRead(0, 9L, 1L, out));
        assertEquals(0.0625D, out.temperatureC(), EPSILON);
        assertFalse(report.sleeping());
    }

    @Test
    void latestSealedTargetCoalescesBeforeExecution() {
        Fixture fixture = fixture(1, limits(8, 8, 8, 3));
        fixture.runtime().sealFrame(new SealedInputFrame(
                5L, 9L, InputWatermarks.ZERO));
        fixture.runtime().sealFrame(new SealedInputFrame(
                15L, 9L, InputWatermarks.ZERO));

        DimensionThermalRuntime.RunReport report = fixture.runtime().runOne();

        assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED, report.status());
        assertEquals(15L, fixture.runtime().lastCompletedTargetTick());
        assertFalse(fixture.runtime().hasReadyWork());
    }

    @Test
    void wholeSolveSetSleepsConservativelyAndWatermarkAdvanceWakesIt() {
        Fixture fixture = fixture(1, limits(8, 8, 8, 2));
        fixture.runtime().sealFrame(new SealedInputFrame(
                5L, 9L, InputWatermarks.ZERO));
        DimensionThermalRuntime.RunReport first = fixture.runtime().runOne();
        assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED, first.status());
        assertFalse(first.sleeping());

        fixture.runtime().sealFrame(new SealedInputFrame(
                10L, 9L, InputWatermarks.ZERO));
        DimensionThermalRuntime.RunReport second = fixture.runtime().runOne();
        assertTrue(second.sleeping());

        fixture.runtime().sealFrame(new SealedInputFrame(
                15L, 9L, InputWatermarks.ZERO));
        DimensionThermalRuntime.RunReport skipped = fixture.runtime().runOne();
        assertEquals(DimensionThermalRuntime.RunStatus.SLEEP_SKIPPED, skipped.status());
        assertTrue(skipped.published());
        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        assertTrue(fixture.publication().tryRead(0, 9L, 1L, out));
        assertEquals(15L, out.sampleTick());

        InputWatermarks changed = new InputWatermarks(1L, 0L, 0L, 0L, 0L);
        fixture.runtime().acknowledgeNonSourceInputs(
                9L, changed, 2L, 2L, true);
        fixture.runtime().sealFrame(new SealedInputFrame(20L, 9L, changed));
        DimensionThermalRuntime.RunReport woke = fixture.runtime().runOne();
        assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED, woke.status());
        assertFalse(woke.sleeping());
    }

    @Test
    void stableUnresolvedTopologyCanSleepUntilNewEvidenceArrives() {
        Fixture fixture = fixture(1, limits(8, 8, 8, 2), false);
        fixture.runtime().sealFrame(new SealedInputFrame(
                5L, 9L, InputWatermarks.ZERO));
        assertFalse(fixture.runtime().runOne().sleeping());
        fixture.runtime().sealFrame(new SealedInputFrame(
                10L, 9L, InputWatermarks.ZERO));

        assertTrue(fixture.runtime().runOne().sleeping());
        assertFalse(fixture.runtime().topologyResolved());
    }

    @Test
    void publicationGrowsToCoverArenaAdmissionBeforePublishing() {
        Fixture fixture = fixture(1, limits(8, 8, 8, 2));
        fixture.arena().allocatePageCells(
                1,
                1,
                new ThermalCellArena.CellSpec[]{new ThermalCellArena.CellSpec(
                        4, 0, 0, 4, 0, 0, 100.0D)},
                new double[]{0.0D});
        assertEquals(1, fixture.publication().capacity());
        fixture.runtime().sealFrame(new SealedInputFrame(
                5L, 9L, InputWatermarks.ZERO));

        DimensionThermalRuntime.RunReport report = fixture.runtime().runOne();

        assertTrue(report.published());
        assertTrue(fixture.publication().capacity() >= fixture.arena().highWaterMark());
    }

    @Test
    void hardWorkCapRefusesAnOversizedCompleteSweep() {
        Fixture fixture = fixture(2, limits(1, 8, 8, 2));
        fixture.runtime().sealFrame(new SealedInputFrame(
                5L, 9L, InputWatermarks.ZERO));

        DimensionThermalRuntime.RunReport report = fixture.runtime().runOne();

        assertEquals(
                DimensionThermalRuntime.RunStatus.WORK_LIMIT_EXCEEDED,
                report.status());
        assertEquals(0L, fixture.runtime().lastCompletedTargetTick());
        assertFalse(report.published());
    }

    @Test
    void unloadRetiresPublicationAndRejectsTheOldGeneration() {
        Fixture fixture = fixture(1, limits(8, 8, 8, 2));
        fixture.runtime().sealFrame(new SealedInputFrame(
                5L, 9L, InputWatermarks.ZERO));
        fixture.runtime().runOne();
        fixture.runtime().unload();

        assertEquals(DimensionThermalRuntime.RunStatus.STALE_GENERATION,
                fixture.runtime().runOne().status());
        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        assertFalse(fixture.publication().tryRead(0, 9L, 1L, out));
    }

    @Test
    void productionReaderRejectsAFormerPublicationAfterTopologyAdvance() {
        Fixture fixture = fixture(1, limits(8, 8, 8, 2));
        fixture.runtime().sealFrame(new SealedInputFrame(
                5L, 9L, InputWatermarks.ZERO));
        assertTrue(fixture.runtime().runOne().published());

        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        assertTrue(fixture.runtime().tryReadPublishedCell(0, out));
        assertEquals(5L, out.sampleTick());

        InputWatermarks changed = new InputWatermarks(1L, 0L, 0L, 0L, 0L);
        assertEquals(DimensionThermalRuntime.AcknowledgeResult.APPLIED,
                fixture.runtime().acknowledgeNonSourceInputs(
                        9L, changed, 2L, 2L, true));
        assertFalse(fixture.runtime().tryReadPublishedCell(0, out));
    }

    @Test
    void diagnosticsNeverReadMutableArenaStateWhileTheWriterIsOwned() {
        Fixture fixture = fixture(2, limits(8, 8, 8, 2));

        DimensionThermalRuntime.Diagnostics idle = fixture.runtime().diagnostics();
        assertFalse(idle.writerBusy());
        assertEquals(2, idle.arenaHighWaterMark());
        assertEquals(2, idle.liveCellCount());
        assertTrue(idle.publicationReservedBytes() > 0L);

        assertTrue(fixture.runtime().tryBeginTopologyUpdate());
        try {
            DimensionThermalRuntime.Diagnostics busy = fixture.runtime().diagnostics();
            assertTrue(busy.writerBusy());
            assertEquals(-1, busy.arenaCapacity());
            assertEquals(-1, busy.arenaHighWaterMark());
            assertEquals(-1, busy.liveCellCount());
            assertEquals(-1, busy.pairOperationCount());
            assertEquals(-1, busy.boundaryOperationCount());
            assertEquals(-1, busy.phaseOperationCount());
        } finally {
            fixture.runtime().cancelTopologyUpdate();
        }

        assertFalse(fixture.runtime().diagnostics().writerBusy());
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
        double[] enthalpies = new double[cellCount];
        for (int slot = 0; slot < cellCount; slot++) {
            cells[slot] = new ThermalCellArena.CellSpec(
                    slot * 4, 0, 0, 4, 0, 0, 100.0D);
        }
        ThermalCellArena arena = new ThermalCellArena(cellCount);
        arena.allocatePageCells(0, 1, cells, enthalpies);
        NodePowerAccumulatorArena accumulators = new NodePowerAccumulatorArena(0);
        ThermalSourceTimeline sources = new ThermalSourceTimeline(
                9L,
                0L,
                16,
                new ThermalSourceRegistry(0, 2, 16, accumulators),
                arena);
        ThermalSweep sweep = new ThermalSweep(
                arena,
                List.of(),
                List.of(),
                new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D));
        ThermalMemoryBudget server = new ThermalMemoryBudget(1_000_000L, 0L);
        QueryPublication publication = QueryPublication.tryCreate(
                server.createDimensionBudget(1_000_000L, 0L),
                Math.max(1, arena.highWaterMark()));
        assertNotNull(publication);
        DimensionThermalRuntime runtime = new DimensionThermalRuntime(
                100L,
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
