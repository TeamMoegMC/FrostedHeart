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
        assertEquals(3, first.retraces());
        assertEquals(0, first.cacheHits());

        RadiationService.MutableSample repeated = new RadiationService.MutableSample();
        service.samplePlayer(9L, 1, 4.5D, 0.0D, 0.5D, repeated);
        assertEquals(expected, repeated.radiantFluxWPerM2(), 1.0e-12D);
        assertEquals(3, repeated.cacheHits());
        assertEquals(0, repeated.retraces());
        assertEquals(3, tracer.traces);
        assertEquals(1, service.sourceCount());

        assertTrue(service.upsertSource(7L, 1, 0.5D, 1.0D, 0.5D, 400.0D, 1.0D));
        RadiationService.MutableSample updated = new RadiationService.MutableSample();
        service.samplePlayer(9L, 1, 4.5D, 0.0D, 0.5D, updated);
        assertEquals(expected * 2.0D, updated.radiantFluxWPerM2(), 1.0e-12D);
        assertEquals(0, updated.cacheHits());
        assertEquals(3, updated.retraces());
        assertEquals(6, tracer.traces);

        assertTrue(service.removeSource(7L));
        assertTrue(service.upsertSource(
                8L, 1, 0.5D, -16.7D, 0.5D, 100.0D, 1.0D));
        RadiationService.MutableSample verticalBoundary =
                new RadiationService.MutableSample();
        service.samplePlayer(10L, 1, 0.5D, -0.8D, 0.5D, verticalBoundary);
        assertTrue(verticalBoundary.radiantFluxWPerM2() > 0.0D);
        assertEquals(1, verticalBoundary.selectedCandidates());
        service.close();
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
            assertEquals(3, sample.retraces());
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

            assertEquals(1, sample.selectedCandidates());
            assertEquals(1, sample.raysTraced());
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
                0.1D, 0.9D, 1.62D);
        long bytes = RadiationService.projectedMaximumBytes(parameters);
        TestTracer tracer = new TestTracer();
        ThermalMemoryBudget refusedBudget = new ThermalMemoryBudget(bytes - 1L, 0L);
        assertNull(RadiationService.tryCreate(parameters, tracer, refusedBudget));

        ThermalMemoryBudget admittedBudget = new ThermalMemoryBudget(bytes, 0L);
        RadiationService service = RadiationService.tryCreate(
                parameters, tracer, admittedBudget);
        assertNotNull(service);
        assertEquals(bytes, service.reservedBytes());
        assertTrue(service.upsertSource(1L, 1, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D));
        assertFalse(service.upsertSource(2L, 1, 1.0D, 0.0D, 0.0D, 1.0D, 1.0D));
        assertEquals(1L, service.sourceAdmissionRefusals());
        RadiationService.MutableSample limited = new RadiationService.MutableSample();
        service.samplePlayer(4L, 1, 2.0D, 0.0D, 0.0D, limited);
        assertTrue((limited.flags() & RadiationService.RADIATION_BUDGET_LIMITED) != 0);
        assertEquals(0.5F, limited.confidence());
        service.close();
        assertEquals(0L, admittedBudget.usedBytes());
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
                0.1D, 0.9D, 1.62D);
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
