/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.radiation;

import com.sun.management.ThreadMXBean;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadiationServiceDroppedWorkloadTest {
    private static final int PLAYER_RECEIVERS = 128;
    private static final int ITEM_RECEIVERS = 64;
    private static final int ITEM_CHURN = 512;
    private static final long STEADY_ALLOCATION_LIMIT_BYTES = 64L * 1024L;

    @Test
    void itemChurnStaysAtSixtyFourWithoutReducingPlayerCapacity() {
        RadiationService.Parameters parameters = productionParameters();
        long projectedBytes = RadiationService.projectedMaximumBytes(parameters);
        ThermalMemoryBudget budget = new ThermalMemoryBudget(projectedBytes, 0L);
        CountingTracer tracer = new CountingTracer();
        try (RadiationService service = RadiationService.tryCreate(
                parameters, tracer, budget)) {
            assertNotNull(service);
            assertTrue(service.upsertSource(
                    1L, 1, 0.5D, 1.0D, 0.5D, 200.0D, 1.0D));
            RadiationService.MutableSample sample =
                    new RadiationService.MutableSample();

            for (int receiver = 0; receiver < PLAYER_RECEIVERS; receiver++) {
                service.samplePlayer(receiver, 1, 4.5D, 0.0D, 0.5D, sample);
                assertEquals(0, sample.flags());
            }
            assertEquals(PLAYER_RECEIVERS, service.playerReceiverCacheSize());

            for (int receiver = 0; receiver < ITEM_RECEIVERS; receiver++) {
                service.sampleItem(
                        10_000L + receiver, 1, 4.5D, 1.0D, 0.5D, sample);
                assertEquals(0, sample.flags());
            }
            for (int receiver = 0; receiver < ITEM_CHURN; receiver++) {
                service.sampleItem(
                        20_000L + receiver, 1, 4.5D, 1.0D, 0.5D, sample);
                assertEquals(0, sample.flags());
            }
            assertEquals(ITEM_RECEIVERS, service.itemReceiverCacheSize());
            assertEquals(PLAYER_RECEIVERS, service.playerReceiverCacheSize());

            long tracesBeforePlayerReuse = tracer.traces;
            for (int receiver = 0; receiver < PLAYER_RECEIVERS; receiver++) {
                service.samplePlayer(receiver, 1, 4.5D, 0.0D, 0.5D, sample);
            }
            assertEquals(tracesBeforePlayerReuse, tracer.traces);
            assertEquals(PLAYER_RECEIVERS, service.playerReceiverCacheSize());
            assertEquals(ITEM_RECEIVERS, service.itemReceiverCacheSize());
            System.out.printf(
                    "FH_T19_WORKLOAD item_receiver_churn=%d item_live=%d "
                            + "player_live=%d player_retrace=0%n",
                    ITEM_CHURN,
                    service.itemReceiverCacheSize(),
                    service.playerReceiverCacheSize());
        }
    }

    @Test
    void steadyReceiversAllocateWithinFixedCeilingAndChurnIsPerClaimBounded() {
        RadiationService.Parameters parameters = productionParameters();
        long projectedBytes = RadiationService.projectedMaximumBytes(parameters);
        ThermalMemoryBudget budget = new ThermalMemoryBudget(projectedBytes, 0L);
        CountingTracer tracer = new CountingTracer();
        try (RadiationService service = RadiationService.tryCreate(
                parameters, tracer, budget)) {
            assertNotNull(service);
            assertTrue(service.upsertSource(
                    1L, 1, 0.5D, 1.0D, 0.5D, 200.0D, 1.0D));
            RadiationService.MutableSample sample =
                    new RadiationService.MutableSample();
            for (int receiver = 0; receiver < ITEM_RECEIVERS; receiver++) {
                service.sampleItem(
                        30_000L + receiver, 1, 4.5D, 1.0D, 0.5D, sample);
            }
            for (int warmup = 0; warmup < 10_000; warmup++) {
                service.sampleItem(
                        30_000L + (warmup & (ITEM_RECEIVERS - 1)),
                        1, 4.5D, 1.0D, 0.5D, sample);
            }

            ThreadMXBean bean = allocationBean();
            long threadId = Thread.currentThread().getId();
            long beforeSteadyBytes = bean.getThreadAllocatedBytes(threadId);
            for (int claim = 0; claim < 100_000; claim++) {
                service.sampleItem(
                        30_000L + (claim & (ITEM_RECEIVERS - 1)),
                        1, 4.5D, 1.0D, 0.5D, sample);
            }
            long steadyBytes = bean.getThreadAllocatedBytes(threadId)
                    - beforeSteadyBytes;
            assertTrue(steadyBytes <= STEADY_ALLOCATION_LIMIT_BYTES,
                    "radiation cache-hit path allocated " + steadyBytes + " bytes");

            long witnessBytes = 80L + parameters.maximumWitnessSectionsPerRay()
                    * 2L * Long.BYTES;
            long projectedItemReceiverBytes = 96L
                    + parameters.itemReceiverLimits().maximumCandidatesPerReceiver()
                    * witnessBytes;
            long perChurnClaimCeiling = projectedItemReceiverBytes + 4_096L;
            long beforeChurnBytes = bean.getThreadAllocatedBytes(threadId);
            for (int claim = 0; claim < ITEM_CHURN; claim++) {
                service.sampleItem(
                        40_000L + claim, 1, 4.5D, 1.0D, 0.5D, sample);
            }
            long churnBytes = bean.getThreadAllocatedBytes(threadId)
                    - beforeChurnBytes;
            assertTrue(churnBytes <= ITEM_CHURN * perChurnClaimCeiling,
                    "receiver churn allocated " + churnBytes
                            + " bytes for " + ITEM_CHURN + " claims");
            assertEquals(ITEM_RECEIVERS, service.itemReceiverCacheSize());
            System.out.printf(
                    "FH_T19_WORKLOAD receiver_cache_hits=%d steady_allocated_bytes=%d "
                            + "steady_ceiling_bytes=%d churn_claims=%d "
                            + "churn_allocated_bytes=%d churn_ceiling_bytes=%d "
                            + "projected_total_bytes=%d%n",
                    100_000,
                    steadyBytes,
                    STEADY_ALLOCATION_LIMIT_BYTES,
                    ITEM_CHURN,
                    churnBytes,
                    ITEM_CHURN * perChurnClaimCeiling,
                    projectedBytes);
        }
    }

    private static RadiationService.Parameters productionParameters() {
        return new RadiationService.Parameters(
                128, 1_024, 128,
                64, 8, 24,
                8, 256,
                16.0D, 0.1D, 0.5D,
                0.1D, 0.9D, 1.62D,
                new RadiationService.ReceiverLimits(64, 32, 4, 4));
    }

    private static ThreadMXBean allocationBean() {
        java.lang.management.ThreadMXBean base =
                ManagementFactory.getThreadMXBean();
        assertTrue(base instanceof ThreadMXBean);
        ThreadMXBean bean = (ThreadMXBean) base;
        assertTrue(bean.isThreadAllocatedMemorySupported());
        if (!bean.isThreadAllocatedMemoryEnabled()) {
            bean.setThreadAllocatedMemoryEnabled(true);
        }
        return bean;
    }

    private static final class CountingTracer
            implements RadiationService.OcclusionTracer {
        private static final long SECTION = RadiationService.packSection(0, 0, 0);
        private long traces;

        @Override
        public void trace(
                double sourceX,
                double sourceY,
                double sourceZ,
                double targetX,
                double targetY,
                double targetZ,
                int maximumSteps,
                RadiationService.MutableTrace result
        ) {
            traces++;
            result.addSection(SECTION, 1L);
            result.finish(RadiationService.TraceStatus.VISIBLE);
        }

        @Override
        public long currentSectionRevision(long packedSectionKey) {
            return packedSectionKey == SECTION
                    ? 1L : RadiationService.NO_SECTION_REVISION;
        }
    }
}
