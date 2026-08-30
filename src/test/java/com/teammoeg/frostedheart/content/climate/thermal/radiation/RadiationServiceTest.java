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

import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadiationServiceTest {
    @Test
    void inverseSquareSamplingReusesRevisionWitnessesWithoutTouchingSources() {
        TestTracer tracer = new TestTracer();
        ThermalMemoryBudget budget = new ThermalMemoryBudget(1_000_000L, 0L);
        RadiationService service = RadiationService.tryCreate(
                parameters(8, 8, 24), tracer, budget);
        assertNotNull(service);
        assertTrue(service.upsertSource(7L, 1, 0.5D, 1.0D, 0.5D, 200.0D, 1.0D));

        RadiationService.MutableSample first = new RadiationService.MutableSample();
        service.samplePlayer(9L, 1, 4.5D, 0.0D, 0.5D, first);
        double expected = 0.0D;
        for (double offset : new double[]{0.1D, 0.9D, 1.62D}) {
            double dy = offset - 1.0D;
            expected += 200.0D / (4.0D * Math.PI * (16.0D + dy * dy)) / 3.0D;
        }
        assertEquals(expected, first.radiantFluxWPerM2(), 1.0e-12D);

        RadiationService.MutableSample repeated = new RadiationService.MutableSample();
        service.samplePlayer(9L, 1, 4.5D, 0.0D, 0.5D, repeated);
        assertEquals(expected, repeated.radiantFluxWPerM2(), 1.0e-12D);
        assertEquals(3, tracer.traces);

        assertTrue(service.upsertSource(7L, 1, 0.5D, 1.0D, 0.5D, 400.0D, 1.0D));
        RadiationService.MutableSample updated = new RadiationService.MutableSample();
        service.samplePlayer(9L, 1, 4.5D, 0.0D, 0.5D, updated);
        assertEquals(expected * 2.0D, updated.radiantFluxWPerM2(), 1.0e-12D);
        assertEquals(6, tracer.traces);

        assertTrue(service.removeSource(7L));
        assertTrue(service.upsertSource(
                8L, 1, 0.5D, -16.7D, 0.5D, 100.0D, 1.0D));
        RadiationService.MutableSample verticalBoundary =
                new RadiationService.MutableSample();
        service.samplePlayer(10L, 1, 0.5D, -0.8D, 0.5D, verticalBoundary);
        assertTrue(verticalBoundary.radiantFluxWPerM2() > 0.0D);
        service.close();
    }

    @Test
    void onePointItemSamplingUsesInverseSquareDistanceAndItsOwnWitness() {
        TestTracer tracer = new TestTracer();
        ThermalMemoryBudget budget = new ThermalMemoryBudget(1_000_000L, 0L);
        try (RadiationService service = RadiationService.tryCreate(
                parameters(8, 8, 24), tracer, budget)) {
            assertNotNull(service);
            assertTrue(service.upsertSource(
                    7L, 1, 0.5D, 1.0D, 0.5D, 200.0D, 1.0D));

            RadiationService.MutableSample first = new RadiationService.MutableSample();
            service.sampleItem(91L, 1, 4.5D, 1.0D, 0.5D, first);
            assertEquals(
                    200.0D / (4.0D * Math.PI * 16.0D),
                    first.radiantFluxWPerM2(),
                    1.0e-12D);
            assertEquals(1, tracer.traces);

            RadiationService.MutableSample repeated =
                    new RadiationService.MutableSample();
            service.sampleItem(91L, 1, 4.5D, 1.0D, 0.5D, repeated);
            assertEquals(first.radiantFluxWPerM2(),
                    repeated.radiantFluxWPerM2(), 0.0D);
            assertEquals(1, tracer.traces);

            RadiationService.MutableSample nearer =
                    new RadiationService.MutableSample();
            service.sampleItem(91L, 1, 2.5D, 1.0D, 0.5D, nearer);
            assertEquals(
                    200.0D / (4.0D * Math.PI * 4.0D),
                    nearer.radiantFluxWPerM2(),
                    1.0e-12D);
            assertEquals(first.radiantFluxWPerM2() * 4.0D,
                    nearer.radiantFluxWPerM2(), 1.0e-12D);
            assertEquals(2, tracer.traces);
        }
    }

    @Test
    void onePointItemWitnessHitsAndRetracesAfterOcclusionRevision() {
        TestTracer tracer = new TestTracer();
        ThermalMemoryBudget budget = new ThermalMemoryBudget(1_000_000L, 0L);
        try (RadiationService service = RadiationService.tryCreate(
                parameters(8, 8, 24), tracer, budget)) {
            assertNotNull(service);
            assertTrue(service.upsertSource(
                    7L, 1, 0.5D, 1.0D, 0.5D, 200.0D, 1.0D));

            RadiationService.MutableSample sample =
                    new RadiationService.MutableSample();
            service.sampleItem(91L, 1, 4.5D, 1.0D, 0.5D, sample);
            assertTrue(sample.radiantFluxWPerM2() > 0.0D);
            service.sampleItem(91L, 1, 4.5D, 1.0D, 0.5D, sample);
            assertEquals(1, tracer.traces);

            tracer.blocked = true;
            tracer.revision++;
            service.sampleItem(91L, 1, 4.5D, 1.0D, 0.5D, sample);
            assertEquals(0.0D, sample.radiantFluxWPerM2());
            assertEquals(2, tracer.traces);
            service.sampleItem(91L, 1, 4.5D, 1.0D, 0.5D, sample);
            assertEquals(2, tracer.traces);

            tracer.blocked = false;
            tracer.revision++;
            service.sampleItem(91L, 1, 4.5D, 1.0D, 0.5D, sample);
            assertTrue(sample.radiantFluxWPerM2() > 0.0D);
            assertEquals(3, tracer.traces);
        }
    }

    @Test
    void itemReceiverAndWorkLimitsRemainIndependentHardCaps() {
        RadiationService.Parameters parameters = new RadiationService.Parameters(
                8, 32, 1,
                8, 2, 6,
                8, 128,
                16.0D, 0.0D, 0.5D,
                0.1D, 0.9D, 1.62D,
                new RadiationService.ReceiverLimits(2, 1, 1, 1));
        TestTracer tracer = new TestTracer();
        ThermalMemoryBudget budget = new ThermalMemoryBudget(1_000_000L, 0L);
        try (RadiationService service = RadiationService.tryCreate(
                parameters, tracer, budget)) {
            assertNotNull(service);
            assertTrue(service.upsertSource(
                    7L, 1, 0.5D, 1.0D, 0.5D, 200.0D, 1.0D));
            assertTrue(service.upsertSource(
                    8L, 1, 1.5D, 1.0D, 0.5D, 100.0D, 1.0D));

            RadiationService.MutableSample sample =
                    new RadiationService.MutableSample();
            for (long receiverKey = 1L; receiverKey <= 3L; receiverKey++) {
                service.sampleItem(
                        receiverKey, 1, 4.5D, 1.0D, 0.5D, sample);
                assertTrue((sample.flags()
                        & RadiationService.RADIATION_BUDGET_LIMITED) != 0);
            }
            assertEquals(3, tracer.traces);
            assertEquals(2, service.itemReceiverCacheSize());
            assertEquals(0, service.playerReceiverCacheSize());

            service.samplePlayer(20L, 1, 4.5D, 0.0D, 0.5D, sample);
            assertEquals(9, tracer.traces);
            assertEquals(0, sample.flags()
                    & RadiationService.RADIATION_BUDGET_LIMITED);
            assertEquals(1, service.playerReceiverCacheSize());
            assertEquals(2, service.itemReceiverCacheSize());
        }
    }

    @Test
    void productionReceiverCapacitiesRemain128PlayersAnd64Items() {
        RadiationService.Parameters parameters = new RadiationService.Parameters(
                128, 1_024, 128,
                64, 8, 24,
                8, 256,
                16.0D, 0.1D, 0.5D,
                0.1D, 0.9D, 1.62D,
                new RadiationService.ReceiverLimits(64, 32, 4, 4));
        TestTracer tracer = new TestTracer();
        ThermalMemoryBudget budget = new ThermalMemoryBudget(4_000_000L, 0L);
        RadiationService service = RadiationService.tryCreate(
                parameters, tracer, budget);
        assertNotNull(service);
        assertTrue(service.upsertSource(
                7L, 1, 0.5D, 1.0D, 0.5D, 200.0D, 1.0D));
        RadiationService.MutableSample sample = new RadiationService.MutableSample();

        for (long receiverKey = 0L; receiverKey <= 128L; receiverKey++) {
            service.samplePlayer(
                    receiverKey, 1, 4.5D, 0.0D, 0.5D, sample);
        }
        assertEquals(128, service.playerReceiverCacheSize());

        for (long receiverKey = 0L; receiverKey <= 64L; receiverKey++) {
            service.sampleItem(
                    receiverKey, 1, 4.5D, 1.0D, 0.5D, sample);
        }
        assertEquals(64, service.itemReceiverCacheSize());
        assertEquals(128, service.playerReceiverCacheSize());

        service.close();
        assertEquals(0, service.playerReceiverCacheSize());
        assertEquals(0, service.itemReceiverCacheSize());
    }

    @Test
    void itemReceiverEvictionCannotDisplacePlayerWitnesses() {
        RadiationService.Parameters parameters = new RadiationService.Parameters(
                8, 32, 1,
                8, 2, 6,
                8, 128,
                16.0D, 0.0D, 0.5D,
                0.1D, 0.9D, 1.62D,
                new RadiationService.ReceiverLimits(1, 2, 1, 1));
        TestTracer tracer = new TestTracer();
        ThermalMemoryBudget budget = new ThermalMemoryBudget(1_000_000L, 0L);
        try (RadiationService service = RadiationService.tryCreate(
                parameters, tracer, budget)) {
            assertNotNull(service);
            assertTrue(service.upsertSource(
                    7L, 1, 0.5D, 1.0D, 0.5D, 200.0D, 1.0D));

            RadiationService.MutableSample sample = new RadiationService.MutableSample();
            service.samplePlayer(1L, 1, 4.5D, 0.0D, 0.5D, sample);
            assertEquals(3, tracer.traces);
            service.sampleItem(2L, 1, 4.5D, 1.0D, 0.5D, sample);
            service.sampleItem(3L, 1, 5.5D, 1.0D, 0.5D, sample);
            assertEquals(5, tracer.traces);

            service.samplePlayer(1L, 1, 4.5D, 0.0D, 0.5D, sample);
            assertEquals(5, tracer.traces);
        }
    }

    @Test
    void sectionRevisionChangeRetracesAndAppliesNewOcclusion() {
        TestTracer tracer = new TestTracer();
        ThermalMemoryBudget budget = new ThermalMemoryBudget(1_000_000L, 0L);
        try (RadiationService service = RadiationService.tryCreate(
                parameters(8, 8, 24), tracer, budget)) {
            assertNotNull(service);
            service.upsertSource(1L, 1, 0.5D, 1.0D, 0.5D, 100.0D, 1.0D);
            RadiationService.MutableSample sample = new RadiationService.MutableSample();
            service.samplePlayer(2L, 1, 3.5D, 0.0D, 0.5D, sample);
            assertTrue(sample.radiantFluxWPerM2() > 0.0D);

            tracer.blocked = true;
            tracer.revision++;
            service.samplePlayer(2L, 1, 3.5D, 0.0D, 0.5D, sample);
            assertEquals(0.0D, sample.radiantFluxWPerM2());
            assertEquals(6, tracer.traces);
        }
    }

    @Test
    void candidateAndRayCapsReturnBoundedLowerConfidenceResult() {
        TestTracer tracer = new TestTracer();
        ThermalMemoryBudget budget = new ThermalMemoryBudget(1_000_000L, 0L);
        try (RadiationService service = RadiationService.tryCreate(
                parameters(1, 8, 1), tracer, budget)) {
            assertNotNull(service);
            service.upsertSource(1L, 1, 0.5D, 1.0D, 0.5D, 100.0D, 1.0D);
            service.upsertSource(2L, 1, 1.5D, 1.0D, 0.5D, 100.0D, 1.0D);
            RadiationService.MutableSample sample = new RadiationService.MutableSample();
            service.samplePlayer(3L, 1, 4.5D, 0.0D, 0.5D, sample);

            assertEquals(1, tracer.traces);
            assertTrue((sample.flags() & RadiationService.RADIATION_BUDGET_LIMITED) != 0);
            assertEquals(0.5F, sample.confidence());
        }
    }

    @Test
    void optionalMemoryAdmissionAndSourceCapacityRemainExplicit() {
        RadiationService.Parameters parameters = new RadiationService.Parameters(
                1, 32, 4,
                1, 1, 3,
                8, 128,
                16.0D, 0.0D, 0.5D,
                0.1D, 0.9D, 1.62D,
                new RadiationService.ReceiverLimits(2, 1, 1, 1));
        long bytes = RadiationService.projectedMaximumBytes(parameters);
        TestTracer tracer = new TestTracer();
        ThermalMemoryBudget refusedBudget = new ThermalMemoryBudget(bytes - 1L, 0L);
        assertNull(RadiationService.tryCreate(parameters, tracer, refusedBudget));

        ThermalMemoryBudget admittedBudget = new ThermalMemoryBudget(bytes, 0L);
        RadiationService service = RadiationService.tryCreate(
                parameters, tracer, admittedBudget);
        assertNotNull(service);
        assertTrue(service.upsertSource(1L, 1, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D));
        assertFalse(service.upsertSource(2L, 1, 1.0D, 0.0D, 0.0D, 1.0D, 1.0D));
        RadiationService.MutableSample limited = new RadiationService.MutableSample();
        service.samplePlayer(4L, 1, 2.0D, 0.0D, 0.0D, limited);
        assertTrue((limited.flags() & RadiationService.RADIATION_BUDGET_LIMITED) != 0);
        assertEquals(0.5F, limited.confidence());
        service.close();
        ThermalMemoryBudget.Reservation releasedBacking = admittedBudget.tryReserve(
                ThermalMemoryBudget.AllocationClass.OPTIONAL, bytes);
        assertNotNull(releasedBacking);
        releasedBacking.close();
    }

    @Test
    void packedSectionsRoundTripSignedCoordinates() {
        long packed = RadiationService.packSection(-2_097_152, -524_288, 2_097_151);
        assertEquals(-2_097_152, RadiationService.sectionX(packed));
        assertEquals(-524_288, RadiationService.sectionY(packed));
        assertEquals(2_097_151, RadiationService.sectionZ(packed));
    }

    private static RadiationService.Parameters parameters(
            int maximumCandidates,
            int maximumCandidateVisits,
            int maximumRays
    ) {
        return new RadiationService.Parameters(
                8, 32, 4,
                maximumCandidateVisits, maximumCandidates, maximumRays,
                8, 128,
                16.0D, 0.0D, 0.5D,
                0.1D, 0.9D, 1.62D,
                new RadiationService.ReceiverLimits(2, 4, 2, 2));
    }

    private static final class TestTracer implements RadiationService.OcclusionTracer {
        private static final long SECTION = RadiationService.packSection(0, 0, 0);
        private long revision = 1L;
        private int traces;
        private boolean blocked;

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
            result.addSection(SECTION, revision);
            result.finish(blocked
                    ? RadiationService.TraceStatus.BLOCKED
                    : RadiationService.TraceStatus.VISIBLE);
        }

        @Override
        public long currentSectionRevision(long packedSectionKey) {
            return packedSectionKey == SECTION
                    ? revision : RadiationService.NO_SECTION_REVISION;
        }
    }
}
