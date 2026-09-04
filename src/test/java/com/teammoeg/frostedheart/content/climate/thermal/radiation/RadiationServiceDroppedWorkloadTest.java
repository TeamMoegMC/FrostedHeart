/* Copyright (c) 2026 TeamMoeg */
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
    private static final int ITEM_CHURN = 512;
    private static final long STEADY_ALLOCATION_LIMIT_BYTES = 64L * 1024L;

    @Test
    void itemChurnStaysBoundedWithoutReducingPlayerCapacity() {
        CountingSource source = new CountingSource();
        CountingTracer tracer = new CountingTracer();
        RadiationService.Parameters parameters = productionParameters();
        try (RadiationService service = RadiationService.tryCreate(
                parameters, source, null, tracer,
                new ThermalMemoryBudget(
                        RadiationService.projectedMaximumBytes(parameters)))) {
            assertNotNull(service);
            RadiationService.MutableSample sample =
                    new RadiationService.MutableSample();
            for (int receiver = 0; receiver < PLAYER_RECEIVERS; receiver++) {
                service.samplePlayer(
                        receiver, 1, 4.5D, 0.0D, 1.62D, 0.5D, sample);
            }
            for (int receiver = 0; receiver < ITEM_CHURN; receiver++) {
                service.sampleItem(
                        10_000L + receiver, 1,
                        4.5D, 1.0D, 0.5D, sample);
            }
            assertEquals(
                    RadiationService.ITEM_MAXIMUM_RECEIVERS,
                    service.itemReceiverCacheSize());
            assertEquals(PLAYER_RECEIVERS, service.playerReceiverCacheSize());

            long tracesBeforePlayerReuse = tracer.traces;
            for (int receiver = 0; receiver < PLAYER_RECEIVERS; receiver++) {
                service.samplePlayer(
                        receiver, 1, 4.5D, 0.0D, 1.62D, 0.5D, sample);
            }
            assertEquals(tracesBeforePlayerReuse, tracer.traces);
        }
    }

    @Test
    void steadyItemReceiversAllocateWithinFixedCeiling() {
        CountingSource source = new CountingSource();
        CountingTracer tracer = new CountingTracer();
        RadiationService.Parameters parameters = productionParameters();
        try (RadiationService service = RadiationService.tryCreate(
                parameters, source, null, tracer,
                new ThermalMemoryBudget(
                        RadiationService.projectedMaximumBytes(parameters)))) {
            assertNotNull(service);
            RadiationService.MutableSample sample =
                    new RadiationService.MutableSample();
            for (int warmup = 0; warmup < 10_000; warmup++) {
                service.sampleItem(
                        20_000L + (warmup & 63), 1,
                        4.5D, 1.0D, 0.5D, sample);
            }
            ThreadMXBean bean = allocationBean();
            long threadId = Thread.currentThread().getId();
            long before = bean.getThreadAllocatedBytes(threadId);
            for (int claim = 0; claim < 100_000; claim++) {
                service.sampleItem(
                        20_000L + (claim & 63), 1,
                        4.5D, 1.0D, 0.5D, sample);
            }
            long allocated = bean.getThreadAllocatedBytes(threadId) - before;
            assertTrue(allocated <= STEADY_ALLOCATION_LIMIT_BYTES,
                    "item receiver cache-hit path allocated " + allocated);
        }
    }

    private static RadiationService.Parameters productionParameters() {
        return new RadiationService.Parameters(
                3_200, 128, 64, 8, 24, 8, 256,
                16.0D, 0.1D, 0.5D, 0.1D, 0.9D, 1.62D);
    }

    private static ThreadMXBean allocationBean() {
        java.lang.management.ThreadMXBean base =
                ManagementFactory.getThreadMXBean();
        assertTrue(base instanceof ThreadMXBean);
        ThreadMXBean bean = (ThreadMXBean) base;
        if (!bean.isThreadAllocatedMemoryEnabled()) {
            bean.setThreadAllocatedMemoryEnabled(true);
        }
        return bean;
    }

    private static final class CountingSource
            implements RadiationService.SourceIndex {
        @Override
        public void visitSection(
                int sectionX,
                int sectionY,
                int sectionZ,
                RadiationService.SourceVisitor visitor
        ) {
            if (sectionX == 0 && sectionY == 0 && sectionZ == 0) {
                visitor.visit(
                        1L, 1L, 0.5D, 1.0D, 0.5D,
                        200.0D, 1.0D);
            }
        }
    }

    private static final class CountingTracer
            implements RadiationService.OcclusionTracer {
        private static final long SECTION =
                RadiationService.packSection(0, 0, 0);
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
                boolean collectWitnesses,
                RadiationService.MutableTrace result
        ) {
            traces++;
            if (collectWitnesses) result.addSection(SECTION, 1L);
            result.finish(RadiationService.TraceStatus.VISIBLE);
        }

        @Override
        public long currentSectionRevision(long packedSectionKey) {
            return packedSectionKey == SECTION
                    ? 1L : RadiationService.NO_SECTION_REVISION;
        }
    }
}
