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
import com.teammoeg.frostedheart.content.climate.thermal.solver.SealedInputFrame;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalTimePolicy;
import com.teammoeg.frostedheart.content.climate.thermal.source.NodePowerAccumulatorArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceTimeline;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalRuntimeCoordinatorTest {
    @Test
    void duplicateRequestsKeepOneQueuedEntryPerDimension() {
        ThermalMemoryBudget server = new ThermalMemoryBudget(1_000_000L, 10_000L);
        ThermalRuntimeCoordinator coordinator = coordinator(server, 2, 2, 0, 100L, 2);
        RuntimeFixture fixture = runtime(1L, 1L, server);
        assertTrue(coordinator.register(fixture.runtime()));

        assertEquals(ThermalRuntimeCoordinator.RequestResult.QUEUED,
                coordinator.request(1L, 1L, false, 0L));
        assertEquals(ThermalRuntimeCoordinator.RequestResult.COALESCED,
                coordinator.request(1L, 1L, false, 1L));
        assertEquals(1, coordinator.readyCount());
        assertEquals("QUEUED", coordinator.mailboxState(1L, 1L));

        ThermalRuntimeCoordinator.DispatchResult result = coordinator.runNext(1L);
        assertEquals(ThermalRuntimeCoordinator.DispatchStatus.EXECUTED, result.status());
        assertEquals(1L, result.runtimeId());
        assertEquals(0, coordinator.readyCount());
        assertEquals("IDLE", coordinator.mailboxState(1L, 1L));
        coordinator.close();
    }

    @Test
    void queueFullLeavesStickyReofferAndEventuallyRunsEveryDimension() {
        ThermalMemoryBudget server = new ThermalMemoryBudget(2_000_000L, 20_000L);
        ThermalRuntimeCoordinator coordinator = coordinator(server, 2, 10, 0, 1_000L, 3);
        for (long id = 1L; id <= 10L; id++) {
            RuntimeFixture fixture = runtime(id, 1L, server);
            assertTrue(coordinator.register(fixture.runtime()));
            ThermalRuntimeCoordinator.RequestResult request = coordinator.request(
                    id, 1L, false, 0L);
            if (id <= 2L) {
                assertEquals(ThermalRuntimeCoordinator.RequestResult.QUEUED, request);
            } else {
                assertEquals(
                        ThermalRuntimeCoordinator.RequestResult.DISPATCH_REOFFER_REQUIRED,
                        request);
                assertTrue(coordinator.dispatchReofferRequired(id, 1L));
            }
        }

        Set<Long> executed = new HashSet<>();
        for (int dispatch = 0; dispatch < 20; dispatch++) {
            ThermalRuntimeCoordinator.DispatchResult result = coordinator.runNext(dispatch);
            if (result.status() == ThermalRuntimeCoordinator.DispatchStatus.EMPTY) {
                break;
            }
            executed.add(result.runtimeId());
        }

        assertEquals(10, executed.size());
        assertEquals(0, coordinator.readyCount());
        for (long id = 1L; id <= 10L; id++) {
            assertFalse(coordinator.dispatchReofferRequired(id, 1L));
        }
        coordinator.close();
    }

    @Test
    void recoveryQuotaSelectsRecoveryBehindNormalTraffic() {
        ThermalMemoryBudget server = new ThermalMemoryBudget(1_000_000L, 10_000L);
        ThermalRuntimeCoordinator coordinator = coordinator(server, 4, 4, 1, 1_000L, 1);
        for (long id = 1L; id <= 4L; id++) {
            RuntimeFixture fixture = runtime(id, 1L, server);
            assertTrue(coordinator.register(fixture.runtime()));
            assertEquals(ThermalRuntimeCoordinator.RequestResult.QUEUED,
                    coordinator.request(id, 1L, id == 4L, 0L));
        }

        assertEquals(1L, coordinator.runNext(0L).runtimeId());
        assertEquals(4L, coordinator.runNext(0L).runtimeId());
        coordinator.close();
    }

    @Test
    void unloadRejectsStaleGenerationAndAllowsReplacementLifecycle() {
        ThermalMemoryBudget server = new ThermalMemoryBudget(1_000_000L, 10_000L);
        ThermalRuntimeCoordinator coordinator = coordinator(server, 2, 2, 0, 100L, 2);
        RuntimeFixture old = runtime(5L, 1L, server);
        assertTrue(coordinator.register(old.runtime()));
        coordinator.request(5L, 1L, false, 0L);

        assertTrue(coordinator.unload(5L, 1L));
        assertEquals(0, coordinator.readyCount());
        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        assertFalse(old.publication().tryRead(0, 1L, 1L, out));
        assertEquals(ThermalRuntimeCoordinator.RequestResult.NOT_REGISTERED,
                coordinator.request(5L, 1L, false, 0L));

        RuntimeFixture replacement = runtime(5L, 2L, server);
        assertTrue(coordinator.register(replacement.runtime()));
        assertEquals(ThermalRuntimeCoordinator.RequestResult.GENERATION_MISMATCH,
                coordinator.request(5L, 1L, false, 0L));
        assertEquals(ThermalRuntimeCoordinator.RequestResult.QUEUED,
                coordinator.request(5L, 2L, false, 0L));
        ThermalRuntimeCoordinator.DispatchResult result = coordinator.runNext(0L);
        assertEquals(2L, result.dimensionGeneration());
        assertEquals(DimensionThermalRuntime.RunStatus.COMPLETED,
                result.runReport().status());
        coordinator.close();
    }

    @Test
    void coordinatorCriticalStorageUsesTheServerReserve() {
        long bytes = ThermalRuntimeCoordinator.estimatedPayloadBytes(2, 2);
        ThermalMemoryBudget server = new ThermalMemoryBudget(bytes, bytes);

        ThermalRuntimeCoordinator coordinator = ThermalRuntimeCoordinator.tryCreate(
                server, 2, 2, 0, 100L, 2);

        assertNotNull(coordinator);
        assertEquals(bytes, server.criticalUsedBytes());
        assertEquals(0L, server.optionalUsedBytes());
        coordinator.close();
        assertEquals(0L, server.usedBytes());
    }

    private static ThermalRuntimeCoordinator coordinator(
            ThermalMemoryBudget server,
            int readyCapacity,
            int maxDimensions,
            int recoveryReserve,
            long promotionTicks,
            int normalRunsPerRecovery
    ) {
        ThermalRuntimeCoordinator coordinator = ThermalRuntimeCoordinator.tryCreate(
                server,
                readyCapacity,
                maxDimensions,
                recoveryReserve,
                promotionTicks,
                normalRunsPerRecovery);
        assertNotNull(coordinator);
        return coordinator;
    }

    private static RuntimeFixture runtime(
            long runtimeId,
            long generation,
            ThermalMemoryBudget server
    ) {
        ThermalCellArena arena = new ThermalCellArena(1);
        arena.allocatePageCells(
                0,
                1,
                new ThermalCellArena.CellSpec[]{new ThermalCellArena.CellSpec(
                        0, 0, 0, 4, 0, 0, 100.0D)},
                new double[]{0.0D});
        ThermalSourceTimeline sources = new ThermalSourceTimeline(
                generation,
                0L,
                4,
                new ThermalSourceRegistry(
                        0, 1, 4, new NodePowerAccumulatorArena(0)),
                arena);
        ThermalSweep sweep = new ThermalSweep(
                arena,
                List.of(),
                List.of(),
                new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D));
        ThermalMemoryBudget dimension = server.createDimensionBudget(100_000L, 1_000L);
        QueryPublication publication = QueryPublication.tryCreate(dimension, 1);
        assertNotNull(publication);
        DimensionThermalRuntime runtime = new DimensionThermalRuntime(
                runtimeId,
                generation,
                0L,
                InputWatermarks.ZERO,
                1L,
                1L,
                true,
                new ThermalTimePolicy(5L, 20L, 2),
                arena,
                sources,
                sweep,
                publication,
                0.0D,
                new DimensionThermalRuntime.Limits(
                        4, 4, 4, 3, 1.0e-9D));
        runtime.sealFrame(new SealedInputFrame(
                5L, generation, InputWatermarks.ZERO));
        return new RuntimeFixture(runtime, publication);
    }

    private record RuntimeFixture(
            DimensionThermalRuntime runtime,
            QueryPublication publication
    ) {
    }
}
